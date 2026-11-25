package com.example.demo.singletype;

import java.time.Instant;
import java.util.UUID;

/** Zero-select event: carries everything needed to run the task, plus the lease it was inserted with. */
public record ReportRequestedEvent(UUID taskId, Instant leasedAt, ReportGenerationPayload payload) {
}
