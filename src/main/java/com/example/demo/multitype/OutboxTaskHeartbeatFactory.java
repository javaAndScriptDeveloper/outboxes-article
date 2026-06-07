package com.example.demo.multitype;

import com.example.demo.Heartbeat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxTaskHeartbeatFactory {

    private final OutboxTaskRepository repository;

    public Heartbeat start(UUID taskId) {
        return new OutboxTaskHeartbeat(taskId, repository);
    }
}
