package com.example.demo.singletype;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

@Value
@Builder
@Jacksonized
public class ReportGenerationPayload {

    Instant from;

    Instant to;
}
