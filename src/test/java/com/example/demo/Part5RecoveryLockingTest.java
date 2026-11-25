package com.example.demo;

import com.example.demo.singletype.ReportGenerationEntity;
import com.example.demo.singletype.ReportGenerationPayload;
import com.example.demo.singletype.ReportGenerationRepository;
import com.example.demo.singletype.ReportGenerationTaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class Part5RecoveryLockingTest extends BaseIntegrationTest {

    @Autowired
    private ReportGenerationTaskService taskService;
    @Autowired
    private ReportGenerationRepository repository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    void recoverySkipsAStuckTaskThatAnotherRecoveryRunHasLocked() throws Exception {
        var task = repository.save(ReportGenerationEntity.builder()
                .state(TaskState.PROCESSING)
                .payload(ReportGenerationPayload.builder()
                        .from(Instant.parse("2024-01-01T00:00:00Z"))
                        .to(Instant.parse("2024-01-31T23:59:59Z"))
                        .build())
                .retryCount(0)
                .executeAt(Instant.now())
                .heartbeatAt(Instant.now().minus(2, ChronoUnit.HOURS))
                .build());

        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var otherRun = CompletableFuture.runAsync(() -> transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.queryForList("SELECT id FROM report_generation_tasks WHERE id = ? FOR UPDATE", task.getId());
            locked.countDown();
            await(release);
        }));
        assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();

        try {
            CompletableFuture.runAsync(taskService::reprocessStuckTasks).get(3, TimeUnit.SECONDS);
            assertThat(repository.findById(task.getId()).orElseThrow().getState())
                    .isEqualTo(TaskState.PROCESSING);
        } finally {
            release.countDown();
            otherRun.get(5, TimeUnit.SECONDS);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
