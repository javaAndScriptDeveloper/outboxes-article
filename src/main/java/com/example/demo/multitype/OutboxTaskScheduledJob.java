package com.example.demo.multitype;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class OutboxTaskScheduledJob {

    private final OutboxTaskService outboxTaskService;

    private static final int BATCH_SIZE = 50;
    private static final Duration TICK_BUDGET = Duration.ofSeconds(30);
    private static final int CLEANUP_BATCH_SIZE = 100_000;
    private static final Duration CLEANUP_BUDGET = Duration.ofMinutes(40); // never run into the next hourly run

    @Scheduled(fixedDelay = 1_000)
    public void run() {
        outboxTaskService.drain(BATCH_SIZE, TICK_BUDGET);
    }

    @Scheduled(fixedDelay = 60_000)
    public void reprocessStuckTasks() {
        outboxTaskService.reprocessStuckTasks();
    }

    @Scheduled(cron = "0 0 * * * *")
    public void cleanupProcessedTasks() {
        outboxTaskService.cleanupProcessedTasks(CLEANUP_BATCH_SIZE, CLEANUP_BUDGET);
    }
}
