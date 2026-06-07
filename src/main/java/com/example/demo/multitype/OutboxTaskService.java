package com.example.demo.multitype;

import com.example.demo.TaskState;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxTaskService {

    private static final int CLEANUP_RETENTION_DAYS = 30;

    private static final Map<Integer, Duration> RETRY_DELAYS = Map.of(
            0, Duration.ofMinutes(1),
            1, Duration.ofMinutes(5),
            2, Duration.ofMinutes(15),
            3, Duration.ofMinutes(30),
            4, Duration.ofHours(1),
            5, Duration.ofHours(2),
            6, Duration.ofHours(4),
            7, Duration.ofHours(8),
            8, Duration.ofHours(12),
            9, Duration.ofDays(1)
    );

    private static final int MAX_RETRY_COUNT = RETRY_DELAYS.size();

    private final OutboxTaskRepository repository;
    private final TransactionTemplate transactionTemplate;
    private final OutboxTaskHeartbeatFactory heartbeatFactory;
    private final ObjectMapper objectMapper;
    private final List<OutboxTaskHandler<?>> handlerList;

    private Map<OutboxTaskType, OutboxTaskHandler<?>> handlers;

    @PostConstruct
    void init() {
        handlers = handlerList.stream()
                .collect(Collectors.toMap(OutboxTaskHandler::getType, Function.identity()));
    }

    public void submit(OutboxTaskType type, Object payload) {
        try {
            var entity = OutboxTaskEntity.builder()
                    .taskType(type)
                    .state(TaskState.PENDING)
                    .payload(objectMapper.writeValueAsString(payload))
                    .retryCount(0)
                    .executeAt(Instant.now())
                    .build();
            repository.save(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize outbox payload", e);
        }
    }

    public void processNextTask() {
        transactionTemplate
                .execute(status -> fetchEligibleTask().map(this::markProcessing))
                .ifPresentOrElse(
                        task -> {
                            try (var heartbeat = heartbeatFactory.start(task.getId())) {
                                try {
                                    dispatch(task);
                                    markCompleted(task);
                                } catch (Exception e) {
                                    markFailed(task, e);
                                    log.error("Failed to process outbox task {} ({}); marked as failed",
                                            task.getId(), task.getTaskType(), e);
                                }
                            }
                        },
                        () -> log.debug("No eligible outbox tasks")
                );
    }

    @Transactional
    public void reprocessStuckTasks() {
        repository.findFirstByStateAndHeartbeatAtLessThanOrderByCreatedAt(
                        TaskState.PROCESSING, Instant.now().minus(1, ChronoUnit.HOURS))
                .ifPresent(task -> {
                    log.warn("Resetting stuck PROCESSING outbox task {} ({}) back to PENDING",
                            task.getId(), task.getTaskType());
                    task.setState(TaskState.PENDING);
                    repository.save(task);
                });
    }

    @Transactional
    public void cleanupProcessedTasks() {
        var cutoff = Instant.now().minus(CLEANUP_RETENTION_DAYS, ChronoUnit.DAYS);
        int deleted = repository.deleteByStateInAndUpdatedAtBefore(List.of(TaskState.COMPLETED), cutoff);
        log.info("Cleanup: removed {} completed outbox tasks older than {} days", deleted, CLEANUP_RETENTION_DAYS);
    }

    @SuppressWarnings("unchecked")
    private void dispatch(OutboxTaskEntity task) throws Exception {
        var handler = (OutboxTaskHandler<Object>) handlers.get(task.getTaskType());
        if (handler == null) {
            throw new IllegalStateException("No handler registered for task type: " + task.getTaskType());
        }
        var payload = objectMapper.readValue(task.getPayload(), handler.payloadClass());
        handler.handle(task.getId(), payload);
    }

    private Optional<OutboxTaskEntity> fetchEligibleTask() {
        return repository.findFirstEligibleTask(
                List.of(TaskState.PENDING, TaskState.FAILED),
                MAX_RETRY_COUNT,
                Instant.now()
        );
    }

    private OutboxTaskEntity markProcessing(OutboxTaskEntity task) {
        task.setState(TaskState.PROCESSING);
        return repository.save(task);
    }

    private void markCompleted(OutboxTaskEntity task) {
        task.setState(TaskState.COMPLETED);
        repository.save(task);
    }

    private void markFailed(OutboxTaskEntity task, Exception exception) {
        int currentRetry = task.getRetryCount();
        task.setState(TaskState.FAILED);
        task.setRetryCount(currentRetry + 1);
        task.setErrorMessage(exception.getMessage());
        task.setExecuteAt(Instant.now().plus(
                RETRY_DELAYS.getOrDefault(currentRetry, RETRY_DELAYS.get(RETRY_DELAYS.size() - 1))));
        repository.save(task);
    }
}
