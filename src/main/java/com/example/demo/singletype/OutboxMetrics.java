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
                        repo -> repo.countByStateIn(List.of(TaskState.PENDING, TaskState.FAILED)))
                .description("Tasks waiting to be processed")
                .register(meterRegistry);
    }
}
