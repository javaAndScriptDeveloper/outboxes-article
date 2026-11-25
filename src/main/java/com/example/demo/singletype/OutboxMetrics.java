package com.example.demo.singletype;

import com.example.demo.TaskState;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxMetrics {

    private static final String TASK_PROCESSED = "outbox.task.processed";
    private static final String OUTCOME_TAG    = "outcome";
    private static final String COMPLETED      = "completed";
    private static final String FAILED         = "failed";

    private final MeterRegistry meterRegistry;
    private final ReportGenerationRepository repository;

    public void recordCompleted() {
        meterRegistry.counter(TASK_PROCESSED, OUTCOME_TAG, COMPLETED).increment();
    }

    public void recordFailed() {
        meterRegistry.counter(TASK_PROCESSED, OUTCOME_TAG, FAILED).increment();
    }

    @PostConstruct
    void registerGauge() {
        Gauge.builder("outbox.tasks.pending", repository,
                        repo -> repo.countByStateInAndRetryCountLessThan(
                                List.of(TaskState.PENDING, TaskState.FAILED), ReportGenerationTaskService.MAX_RETRY_COUNT))
                .description("Tasks that can still run")
                .register(meterRegistry);
        // FAILED rows that used up their retries stay as an audit trail; counting them as "pending"
        // would make the backlog grow forever. They get their own gauge, which should be zero.
        Gauge.builder("outbox.tasks.dead", repository,
                        repo -> repo.countByStateAndRetryCountGreaterThanEqual(
                                TaskState.FAILED, ReportGenerationTaskService.MAX_RETRY_COUNT))
                .description("Tasks that exhausted their retries and need a human")
                .register(meterRegistry);
    }
}
