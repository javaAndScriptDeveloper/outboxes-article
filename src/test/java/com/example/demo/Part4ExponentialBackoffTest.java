package com.example.demo;

import com.example.demo.singletype.ReportGenerationEntity;
import com.example.demo.singletype.ReportGenerationPayload;
import com.example.demo.singletype.ReportGenerationRepository;
import com.example.demo.singletype.ReportGenerationTaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class Part4ExponentialBackoffTest extends BaseIntegrationTest {

    @Autowired
    private ReportGenerationTaskService taskService;

    @Autowired
    private ReportGenerationRepository repository;

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    void failedTaskWithFutureExecuteAtIsNotPickedUp() {
        var task = repository.save(ReportGenerationEntity.builder()
                .state(TaskState.FAILED)
                .payload(ReportGenerationPayload.builder()
                        .from(Instant.parse("2024-01-01T00:00:00Z"))
                        .to(Instant.parse("2024-01-31T23:59:59Z"))
                        .build())
                .retryCount(1)
                .executeAt(Instant.now().plusSeconds(3600))
                .build());

        taskService.fetchAndProcess();

        var found = repository.findById(task.getId()).orElseThrow();
        assertThat(found.getState()).isEqualTo(TaskState.FAILED);
        assertThat(found.getRetryCount()).isEqualTo(1);
    }

    @Test
    void failedTaskWithPastExecuteAtIsPickedUpAndCompleted() {
        var task = repository.save(ReportGenerationEntity.builder()
                .state(TaskState.FAILED)
                .payload(ReportGenerationPayload.builder()
                        .from(Instant.parse("2024-01-01T00:00:00Z"))
                        .to(Instant.parse("2024-01-31T23:59:59Z"))
                        .build())
                .retryCount(1)
                .executeAt(Instant.now().minusSeconds(60))
                .build());

        taskService.fetchAndProcess();

        var found = repository.findById(task.getId()).orElseThrow();
        assertThat(found.getState()).isEqualTo(TaskState.COMPLETED);
    }
}
