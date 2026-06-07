package com.example.demo.multitype;

import java.util.UUID;

public interface OutboxTaskHandler<P> {

    OutboxTaskType getType();

    Class<P> payloadClass();

    void handle(UUID taskId, P payload) throws Exception;
}
