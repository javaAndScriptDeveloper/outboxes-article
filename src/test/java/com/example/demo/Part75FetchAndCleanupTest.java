package com.example.demo;

import com.example.demo.multitype.EmailNotificationPayload;
import com.example.demo.multitype.OutboxTaskEntity;
import com.example.demo.multitype.OutboxTaskRepository;
import com.example.demo.multitype.OutboxTaskService;
import com.example.demo.multitype.OutboxTaskType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class Part75FetchAndCleanupTest extends BaseIntegrationTest {

    @Autowired
    private OutboxTaskService service;
    @Autowired
    private OutboxTaskRepository repository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    @AfterEach
    void clean() {
        jdbcTemplate.update("DELETE FROM outbox_tasks");
    }

    @Test
    void claimBatchTakesTheOldestBatchSizeTasksAndMarksThemProcessing() {
        var ids = submitEmails(5);

        var claimed = repository.claimBatch(2, OutboxTaskService.MAX_RETRY_COUNT);

        // UPDATE ... RETURNING does not guarantee order; drain() sorts by seq in memory
        assertThat(claimed).extracting(OutboxTaskEntity::getId).containsExactlyInAnyOrder(ids.get(0), ids.get(1));
        assertThat(claimed).allMatch(t -> t.getState() == TaskState.PROCESSING && t.getHeartbeatAt() != null);
        assertThat(repository.findAllById(ids.subList(2, 5))).allMatch(t -> t.getState() == TaskState.PENDING);
    }

    @Test
    void claimBatchSkipsRowsAnotherWorkerHasLockedInsteadOfWaiting() throws Exception {
        var ids = submitEmails(3);

        var claimed = whileRowIsLocked(ids.getFirst(),
                () -> repository.claimBatch(10, OutboxTaskService.MAX_RETRY_COUNT));

        assertThat(claimed).extracting(OutboxTaskEntity::getId).containsExactlyInAnyOrder(ids.get(1), ids.get(2));
    }

    @Test
    void drainLoopProcessesTheWholeBacklogInOneTick() {
        var ids = submitEmails(5);

        int processed = service.drain(2, Duration.ofSeconds(30));

        assertThat(processed).isEqualTo(5);
        assertThat(repository.findAllById(ids)).allMatch(t -> t.getState() == TaskState.COMPLETED);
    }

    @Test
    void cleanupDeletesOnlyOldCompletedRowsInSmallBatches() {
        var old = submitEmails(5);
        old.forEach(id -> age(id, "COMPLETED"));
        var recent = submitEmails(1).getFirst();
        jdbcTemplate.update("UPDATE outbox_tasks SET state = 'COMPLETED' WHERE id = ?", recent);
        var oldPending = submitEmails(1).getFirst();
        age(oldPending, "PENDING");

        int deleted = service.cleanupProcessedTasks(2, Duration.ofMinutes(1));

        assertThat(deleted).isEqualTo(5);
        assertThat(repository.findAll()).extracting(OutboxTaskEntity::getId).containsExactlyInAnyOrder(recent, oldPending);
    }

    @Test
    void overlappingCleanupRunsSkipEachOthersRowsInsteadOfWaiting() throws Exception {
        var old = submitEmails(3);
        old.forEach(id -> age(id, "COMPLETED"));

        int deleted = whileRowIsLocked(old.getFirst(), () -> service.cleanupProcessedTasks(10, Duration.ofMinutes(1)));

        assertThat(deleted).isEqualTo(2);
    }

    private List<UUID> submitEmails(int count) {
        return IntStream.range(0, count).mapToObj(i -> {
            service.submit(OutboxTaskType.EMAIL_NOTIFICATION, EmailNotificationPayload.builder()
                    .recipient("user" + i + "@example.com").subject("s").body("b").build());
            return jdbcTemplate.queryForObject("SELECT id FROM outbox_tasks ORDER BY seq DESC LIMIT 1", UUID.class);
        }).toList();
    }

    private void age(UUID id, String state) {
        jdbcTemplate.update("UPDATE outbox_tasks SET state = ?::task_state, updated_at = now() - interval '40 days' WHERE id = ?",
                state, id);
    }

    private <T> T whileRowIsLocked(UUID id, java.util.concurrent.Callable<T> action) throws Exception {
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var holder = CompletableFuture.runAsync(() -> transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.queryForList("SELECT id FROM outbox_tasks WHERE id = ? FOR UPDATE", id);
            locked.countDown();
            try {
                release.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
        assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return action.call();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }).get(3, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            holder.get(5, TimeUnit.SECONDS);
        }
    }
}
