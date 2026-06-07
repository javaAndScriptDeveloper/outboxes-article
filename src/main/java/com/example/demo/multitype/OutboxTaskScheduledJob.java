package com.example.demo.multitype;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxTaskScheduledJob {

    private final OutboxTaskService outboxTaskService;

    @Scheduled(fixedDelay = 1_000)
    public void run() {
        outboxTaskService.processNextTask();
    }

    @Scheduled(fixedDelay = 60_000)
    public void reprocessStuckTasks() {
        outboxTaskService.reprocessStuckTasks();
    }

    @Scheduled(fixedDelay = 3_600_000)
    public void cleanupProcessedTasks() {
        outboxTaskService.cleanupProcessedTasks();
    }
}
