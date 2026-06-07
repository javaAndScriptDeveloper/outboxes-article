package com.example.demo;

import com.example.demo.singletype.ReportGenerationEntity;
import com.example.demo.singletype.ReportGenerationPayload;
import com.example.demo.singletype.ReportGenerationRepository;
import com.example.demo.singletype.ReportGenerationTaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class Part2SkipLockedConcurrencyTest extends BaseIntegrationTest {

    @Autowired
    private ReportGenerationTaskService taskService;

    @Autowired
    private ReportGenerationRepository repository;

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    void twoWorkersConcurrentlyClaimDifferentTasks() throws InterruptedException {
        var now = Instant.now();
        repository.saveAll(List.of(
                ReportGenerationEntity.builder()
                        .state(TaskState.PENDING)
                        .payload(ReportGenerationPayload.builder()
                                .from(now.minusSeconds(7200))
                                .to(now.minusSeconds(3600))
                                .build())
                        .retryCount(0).executeAt(now).build(),
                ReportGenerationEntity.builder()
                        .state(TaskState.PENDING)
                        .payload(ReportGenerationPayload.builder()
                                .from(now.minusSeconds(3600))
                                .to(now)
                                .build())
                        .retryCount(0).executeAt(now).build()
        ));

        var latch = new CountDownLatch(2);

        Thread.ofVirtual().start(() -> {
            taskService.fetchAndProcess();
            latch.countDown();
        });
        Thread.ofVirtual().start(() -> {
            taskService.fetchAndProcess();
            latch.countDown();
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();

        var all = repository.findAll();
        assertThat(all)
                .hasSize(2)
                .allMatch(t -> t.getState() == TaskState.COMPLETED);
    }
}
