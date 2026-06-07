package com.example.demo.singletype;

import com.example.demo.TaskState;
import io.micrometer.core.annotation.Timed;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGenerationTaskService {

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

    private final ReportGenerationRepository repository;
    private final TransactionTemplate transactionTemplate;
    private final ReportGenerationTaskHeartbeatFactory heartbeatFactory;
    private final OutboxMetrics metrics;

    @Timed(value = "outbox.task.duration", percentiles = {0.5, 0.95, 0.99},
            description = "Time spent processing one outbox task")
    public void fetchAndProcess() {
        transactionTemplate
                .execute(status -> fetchEligibleTask().map(this::markProcessing))
                .ifPresentOrElse(
                        task -> {
                            try (var heartbeat = heartbeatFactory.start(task.getId())) {
                                try {
                                    generateReport(task);
                                    markCompleted(task);
                                } catch (Exception e) {
                                    markFailed(task, e);
                                    log.error("Failed to process task {}; marked as failed", task.getId(), e);
                                }
                            }
                        },
                        () -> log.debug("No eligible tasks")
                );
    }

    @Transactional
    public void reprocessStuckTasks() {
        repository.findFirstByStateAndHeartbeatAtLessThanOrderByCreatedAt(
                        TaskState.PROCESSING, Instant.now().minus(1, ChronoUnit.HOURS))
                .ifPresent(task -> {
                    log.warn("Resetting stuck PROCESSING task {} back to PENDING", task.getId());
                    task.setState(TaskState.PENDING);
                    repository.save(task);
                });
    }

    @Transactional
    public void cleanup() {
        var cutoff = Instant.now().minus(CLEANUP_RETENTION_DAYS, ChronoUnit.DAYS);
        int deleted = repository.deleteByStateInAndUpdatedAtBefore(List.of(TaskState.COMPLETED), cutoff);
        log.info("Cleanup: removed {} completed tasks older than {} days", deleted, CLEANUP_RETENTION_DAYS);
    }

    private void generateReport(ReportGenerationEntity task) {
        log.info("Generating report for task {} (from={} to={})",
                task.getId(), task.getPayload().getFrom(), task.getPayload().getTo());
    }

    private Optional<ReportGenerationEntity> fetchEligibleTask() {
        return repository.findFirstEligibleTask(
                List.of(TaskState.PENDING, TaskState.FAILED),
                MAX_RETRY_COUNT,
                Instant.now()
        );
    }

    private ReportGenerationEntity markProcessing(ReportGenerationEntity task) {
        task.setState(TaskState.PROCESSING);
        return repository.save(task);
    }

    private void markCompleted(ReportGenerationEntity task) {
        task.setState(TaskState.COMPLETED);
        repository.save(task);
        metrics.recordCompleted();
    }

    private void markFailed(ReportGenerationEntity task, Exception e) {
        int currentRetry = task.getRetryCount();
        task.setState(TaskState.FAILED);
        task.setRetryCount(currentRetry + 1);
        task.setErrorMessage(e.getMessage());
        task.setExecuteAt(Instant.now().plus(
                RETRY_DELAYS.getOrDefault(currentRetry, RETRY_DELAYS.get(RETRY_DELAYS.size() - 1))));
        repository.save(task);
        metrics.recordFailed();
    }
}
