package com.example.demo;

import com.example.demo.singletype.CompletionBuffer;
import com.example.demo.singletype.ReportGenerationCommand;
import com.example.demo.singletype.ReportGenerationEntity;
import com.example.demo.singletype.ReportGenerationPayload;
import com.example.demo.singletype.ReportGenerationRepository;
import com.example.demo.singletype.ReportGenerationService;
import com.example.demo.singletype.ReportRequestedEvent;
import com.example.demo.singletype.ZeroSelectReportListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class Part25ApplicationEventsTest extends BaseIntegrationTest {

    private static final ReportGenerationPayload PAYLOAD = ReportGenerationPayload.builder()
            .from(Instant.parse("2024-01-01T00:00:00Z"))
            .to(Instant.parse("2024-01-31T23:59:59Z"))
            .build();

    @Autowired
    private ReportGenerationService service;
    @Autowired
    private ReportGenerationRepository repository;
    @Autowired
    private ZeroSelectReportListener zeroSelectListener;
    @Autowired
    private CompletionBuffer completionBuffer;
    @Autowired
    @Qualifier("outboxExecutor")
    private ThreadPoolTaskExecutor outboxExecutor;
    @Autowired
    private ApplicationContext context;

    @BeforeEach
    @AfterEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void submittedTaskIsProcessedAfterCommitOnTheDedicatedOutboxPool() {
        long before = outboxExecutor.getThreadPoolExecutor().getCompletedTaskCount();

        service.submit(command());

        awaitTrue(() -> repository.findAll().getFirst().getState() == TaskState.COMPLETED);
        awaitTrue(() -> outboxExecutor.getThreadPoolExecutor().getCompletedTaskCount() > before);
        assertThat(outboxExecutor.getThreadNamePrefix()).isEqualTo("outbox-");
    }

    @Test
    void declaringTheOutboxPoolKeepsSpringBootsDefaultExecutorForOtherAsyncWork() {
        assertThat(context.containsBean("applicationTaskExecutor")).isTrue();
    }

    @Test
    void zeroSelectTaskIsInsertedAlreadyLeasedAndCompletedFromTheEventPayload() {
        service.submitZeroSelect(command());

        var task = repository.findAll().getFirst();
        assertThat(task.getHeartbeatAt()).isNotNull();
        awaitTrue(() -> repository.findById(task.getId()).orElseThrow().getState() == TaskState.COMPLETED);
    }

    @Test
    void staleZeroSelectEventDoesNotRunATaskThatRecoveryAlreadyHandedBack() {
        var leasedAt = Instant.now().minus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MICROS);
        var task = repository.save(ReportGenerationEntity.builder()
                .state(TaskState.PENDING) // recovery reset it while the event waited in the queue
                .payload(PAYLOAD)
                .retryCount(0)
                .executeAt(Instant.now())
                .heartbeatAt(leasedAt)
                .build());
        long before = outboxExecutor.getThreadPoolExecutor().getCompletedTaskCount();

        zeroSelectListener.onReportRequested(new ReportRequestedEvent(task.getId(), leasedAt, PAYLOAD));

        awaitTrue(() -> outboxExecutor.getThreadPoolExecutor().getCompletedTaskCount() > before);
        assertThat(repository.findById(task.getId()).orElseThrow().getState()).isEqualTo(TaskState.PENDING);
    }

    @Test
    void bufferedCompletionsAreWrittenInOneFlush() {
        List<UUID> ids = IntStream.range(0, 3).mapToObj(i -> repository.save(ReportGenerationEntity.builder()
                .state(TaskState.PROCESSING)
                .payload(PAYLOAD)
                .retryCount(0)
                .executeAt(Instant.now())
                .heartbeatAt(Instant.now())
                .build()).getId()).toList();

        ids.forEach(completionBuffer::add);
        completionBuffer.flush();

        assertThat(repository.findAllById(ids)).allMatch(t -> t.getState() == TaskState.COMPLETED);
    }

    @Test
    void bulkSubmissionsAreSpreadOverTheNextTenMinutes() {
        var start = Instant.now();

        service.submitBulk(IntStream.range(0, 20).mapToObj(i -> command()).toList());

        var executeAts = repository.findAll().stream().map(ReportGenerationEntity::getExecuteAt).toList();
        assertThat(executeAts).hasSize(20)
                .allMatch(at -> !at.isBefore(start.minusSeconds(1)) && at.isBefore(start.plus(Duration.ofMinutes(10)).plusSeconds(1)));
        assertThat(executeAts.stream().distinct().count()).isGreaterThan(1);
    }

    private static ReportGenerationCommand command() {
        return new ReportGenerationCommand(PAYLOAD.getFrom(), PAYLOAD.getTo());
    }

    private static void awaitTrue(BooleanSupplier condition) {
        var deadline = Instant.now().plusSeconds(10);
        while (!condition.getAsBoolean()) {
            assertThat(Instant.now()).as("condition not met within 10s").isBefore(deadline);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }
}
