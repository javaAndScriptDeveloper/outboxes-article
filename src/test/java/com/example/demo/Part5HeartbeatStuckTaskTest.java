package com.example.demo;

import com.example.demo.singletype.ReportGenerationEntity;
import com.example.demo.singletype.ReportGenerationPayload;
import com.example.demo.singletype.ReportGenerationRepository;
import com.example.demo.singletype.ReportGenerationTaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class Part5HeartbeatStuckTaskTest extends BaseIntegrationTest {

    @Autowired
    private ReportGenerationTaskService taskService;

    @Autowired
    private ReportGenerationRepository repository;

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    void stuckProcessingTaskIsResetToPending() {
        var staleHeartbeat = Instant.now().minus(2, ChronoUnit.HOURS);
        var task = repository.save(ReportGenerationEntity.builder()
                .state(TaskState.PROCESSING)
                .payload(ReportGenerationPayload.builder()
                        .from(Instant.parse("2024-01-01T00:00:00Z"))
                        .to(Instant.parse("2024-01-31T23:59:59Z"))
                        .build())
                .retryCount(0)
                .executeAt(Instant.now())
                .heartbeatAt(staleHeartbeat)
                .build());

        taskService.reprocessStuckTasks();

        var updated = repository.findById(task.getId()).orElseThrow();
        assertThat(updated.getState()).isEqualTo(TaskState.PENDING);
    }

    @Test
    void recentlyHeartbeatedTaskIsNotReset() {
        var recentHeartbeat = Instant.now().minus(5, ChronoUnit.MINUTES);
        var task = repository.save(ReportGenerationEntity.builder()
                .state(TaskState.PROCESSING)
                .payload(ReportGenerationPayload.builder()
                        .from(Instant.parse("2024-01-01T00:00:00Z"))
                        .to(Instant.parse("2024-01-31T23:59:59Z"))
                        .build())
                .retryCount(0)
                .executeAt(Instant.now())
                .heartbeatAt(recentHeartbeat)
                .build());

        taskService.reprocessStuckTasks();

        var updated = repository.findById(task.getId()).orElseThrow();
        assertThat(updated.getState()).isEqualTo(TaskState.PROCESSING);
    }
}
