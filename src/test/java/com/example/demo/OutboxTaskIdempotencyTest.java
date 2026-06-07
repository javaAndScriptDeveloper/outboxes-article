package com.example.demo;

import com.example.demo.multitype.GeneratedReportRepository;
import com.example.demo.multitype.OutboxTaskRepository;
import com.example.demo.multitype.OutboxTaskService;
import com.example.demo.multitype.OutboxTaskType;
import com.example.demo.singletype.ReportGenerationPayload;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxTaskIdempotencyTest extends BaseIntegrationTest {

    @Autowired
    private OutboxTaskService outboxTaskService;

    @Autowired
    private OutboxTaskRepository outboxTaskRepository;

    @Autowired
    private GeneratedReportRepository generatedReportRepository;

    @AfterEach
    void tearDown() {
        generatedReportRepository.deleteAll();
        outboxTaskRepository.deleteAll();
    }

    @Test
    void processingTheSameTaskTwiceProducesExactlyOneReport() {
        outboxTaskService.submit(OutboxTaskType.REPORT_GENERATION,
                ReportGenerationPayload.builder()
                        .from(Instant.parse("2024-01-01T00:00:00Z"))
                        .to(Instant.parse("2024-01-31T23:59:59Z"))
                        .build());
        var taskId = outboxTaskRepository.findAll().getFirst().getId();

        outboxTaskService.processNextTask();
        assertThat(generatedReportRepository.count()).isEqualTo(1);

        var task = outboxTaskRepository.findById(taskId).orElseThrow();
        task.setState(TaskState.PENDING);
        outboxTaskRepository.save(task);

        outboxTaskService.processNextTask();

        assertThat(generatedReportRepository.count()).isEqualTo(1);
        assertThat(generatedReportRepository.findAll().getFirst().getIdempotencyKey()).isEqualTo(taskId);
    }
}
