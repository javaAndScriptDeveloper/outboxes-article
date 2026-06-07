package com.example.demo.multitype.handler;

import com.example.demo.multitype.EmailNotificationPayload;
import com.example.demo.multitype.OutboxTaskHandler;
import com.example.demo.multitype.OutboxTaskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class EmailNotificationOutboxHandler implements OutboxTaskHandler<EmailNotificationPayload> {

    @Override
    public OutboxTaskType getType() {
        return OutboxTaskType.EMAIL_NOTIFICATION;
    }

    @Override
    public Class<EmailNotificationPayload> payloadClass() {
        return EmailNotificationPayload.class;
    }

    @Override
    public void handle(UUID taskId, EmailNotificationPayload payload) {
        log.info("Sending email to {} with subject '{}' (outbox task {})",
                payload.getRecipient(), payload.getSubject(), taskId);
    }
}
