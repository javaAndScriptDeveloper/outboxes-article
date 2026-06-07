package com.example.demo.singletype;

import com.example.demo.Heartbeat;

import java.time.Instant;
import java.util.UUID;

public class ReportGenerationTaskHeartbeat extends Heartbeat {

    ReportGenerationTaskHeartbeat(UUID taskId, ReportGenerationRepository repository) {
        super(() -> repository.updateHeartbeatAt(taskId, Instant.now()));
    }
}
