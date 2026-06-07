package com.example.demo.singletype;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportGenerationScheduledJob {

    private final ReportGenerationTaskService taskService;

    @Scheduled(fixedDelay = 1_000)
    public void run() {
        taskService.fetchAndProcess();
    }

    @Scheduled(fixedDelay = 60_000)
    public void reprocessStuckTasks() {
        taskService.reprocessStuckTasks();
    }

    @Scheduled(fixedDelay = 3_600_000)
    public void cleanup() {
        taskService.cleanup();
    }
}
