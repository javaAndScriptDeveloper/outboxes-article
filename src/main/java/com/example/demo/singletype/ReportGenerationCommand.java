package com.example.demo.singletype;

import lombok.Value;

import java.time.Instant;

@Value
public class ReportGenerationCommand {

    Instant from;

    Instant to;
}
