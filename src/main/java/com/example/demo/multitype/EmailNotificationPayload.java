package com.example.demo.multitype;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class EmailNotificationPayload {

    String recipient;

    String subject;

    String body;
}
