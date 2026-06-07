package com.example.demo;

import com.example.demo.singletype.ReportGenerationEntity;
import com.example.demo.singletype.ReportGenerationPayload;
import com.example.demo.singletype.ReportGenerationRepository;
import com.example.demo.singletype.ReportGenerationTaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ReportGenerationTaskServiceProcessingStatusTest extends BaseIntegrationTest {

    @Autowired
    private ReportGenerationTaskService reportGenerationTaskService;

    @Autowired
    private ReportGenerationRepository reportGenerationRepository;

    @BeforeEach
    void setUp() {
        var now = Instant.now();
        var tasks = IntStream.range(0, 100)
                .mapToObj(i -> ReportGenerationEntity.builder()
                        .state(TaskState.PENDING)
                        .payload(ReportGenerationPayload.builder()
                                .from(now.minus(i + 1, ChronoUnit.DAYS))
                                .to(now.minus(i, ChronoUnit.DAYS))
                                .build())
                        .retryCount(0)
                        .executeAt(now)
                        .build())
                .toList();
        reportGenerationRepository.saveAll(tasks);
    }

    @AfterEach
    void tearDown() {
        reportGenerationRepository.deleteAll();
    }

    @Test
    void shouldFetchOldestPendingTaskProcessAndMarkCompleted() {
        var allBefore = reportGenerationRepository.findAll();
        assertThat(allBefore)
                .hasSize(100)
                .allMatch(t -> t.getState() == TaskState.PENDING);

        reportGenerationTaskService.fetchAndProcess();

        var allAfter = reportGenerationRepository.findAll();
        assertThat(allAfter).hasSize(100);

        var completed = allAfter.stream()
                .filter(t -> t.getState() == TaskState.COMPLETED)
                .toList();
        assertThat(completed).hasSize(1);

        var pending = allAfter.stream()
                .filter(t -> t.getState() == TaskState.PENDING)
                .toList();
        assertThat(pending).hasSize(99);

        assertThat(completed.getFirst().getVersion()).isEqualTo(2L);
    }
}
