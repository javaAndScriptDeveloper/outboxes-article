package com.example.demo.singletype;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

@Value
@JsonIgnoreProperties(ignoreUnknown = true) // rows written by a newer version still load
@Builder
@Jacksonized
public class ReportGenerationPayload {

    Instant from;

    Instant to;
}
