package com.example.demo.multitype;

import com.example.demo.Heartbeat;

import java.time.Instant;
import java.util.UUID;

public class OutboxTaskHeartbeat extends Heartbeat {

    OutboxTaskHeartbeat(UUID taskId, OutboxTaskRepository repository) {
        super(() -> repository.updateHeartbeatAt(taskId, Instant.now()));
    }
}
