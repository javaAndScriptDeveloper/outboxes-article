package com.example.demo;

import com.example.demo.singletype.ReportGenerationEntity;
import com.example.demo.singletype.ReportGenerationPayload;
import com.example.demo.singletype.ReportGenerationRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class Part11MonitoringTest extends BaseIntegrationTest {

    @Autowired
    private ReportGenerationRepository repository;
    @Autowired
    private MeterRegistry meterRegistry;

    @BeforeEach
    void cleanBefore() {
        repository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    void pendingGaugeCountsOnlyTasksThatCanStillRunAndDeadGaugeCountsExhaustedOnes() {
        save(TaskState.PENDING, 0);
        save(TaskState.FAILED, 3);
        save(TaskState.FAILED, 10);

        assertThat(meterRegistry.get("outbox.tasks.pending").gauge().value()).isEqualTo(2.0);
        assertThat(meterRegistry.get("outbox.tasks.dead").gauge().value()).isEqualTo(1.0);
    }

    private void save(TaskState state, int retryCount) {
        repository.save(ReportGenerationEntity.builder()
                .state(state)
                .payload(ReportGenerationPayload.builder()
                        .from(Instant.parse("2024-01-01T00:00:00Z"))
                        .to(Instant.parse("2024-01-31T23:59:59Z"))
                        .build())
                .retryCount(retryCount)
                .executeAt(Instant.now())
                .build());
    }
}
