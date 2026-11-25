package com.example.demo.singletype;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Zero-select outbox (Part 2.5): executes straight from the event payload, never SELECTs the task. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ZeroSelectReportListener {

    private final ReportGenerationRepository repository;
    private final ReportGenerationTaskHeartbeatFactory heartbeatFactory;

    @Async("outboxExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReportRequested(ReportRequestedEvent event) {
        // UPDATE ... SET heartbeat_at = now()
        //  WHERE id = :id AND state = 'PROCESSING' AND heartbeat_at = :leasedAt
        if (repository.renewLease(event.taskId(), event.leasedAt()) == 0) {
            log.info("Task {} was handed back to the poller while its event waited — skipping", event.taskId());
            return;
        }
        try (var heartbeat = heartbeatFactory.start(event.taskId())) {
            generateReport(event.payload());
            repository.markCompleted(event.taskId());              // UPDATE ... WHERE id = :id
        } catch (Exception e) {
            repository.markFailed(event.taskId(), e.getMessage()); // the poller retries it later
        }
    }

    private void generateReport(ReportGenerationPayload payload) {
        log.info("Generating report from {} to {} (zero-select)", payload.getFrom(), payload.getTo());
    }
}
