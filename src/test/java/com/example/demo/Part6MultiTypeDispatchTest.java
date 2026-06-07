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

class Part6MultiTypeDispatchTest extends BaseIntegrationTest {

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
    void reportGenerationTaskIsDispatchedToCorrectHandlerAndCreatesReport() {
        outboxTaskService.submit(OutboxTaskType.REPORT_GENERATION,
                ReportGenerationPayload.builder()
                        .from(Instant.parse("2024-01-01T00:00:00Z"))
                        .to(Instant.parse("2024-01-31T23:59:59Z"))
                        .build());

        outboxTaskService.processNextTask();

        assertThat(generatedReportRepository.count()).isEqualTo(1);

        var task = outboxTaskRepository.findAll().getFirst();
        assertThat(task.getState()).isEqualTo(TaskState.COMPLETED);
    }

    @Test
    void emailNotificationTaskIsDispatchedWithoutError() {
        outboxTaskService.submit(OutboxTaskType.EMAIL_NOTIFICATION,
                com.example.demo.multitype.EmailNotificationPayload.builder()
                        .recipient("test@example.com")
                        .subject("Hello")
                        .body("Test message")
                        .build());

        outboxTaskService.processNextTask();

        var task = outboxTaskRepository.findAll().getFirst();
        assertThat(task.getState()).isEqualTo(TaskState.COMPLETED);
        assertThat(generatedReportRepository.count()).isZero();
    }
}
