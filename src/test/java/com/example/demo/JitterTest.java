package com.example.demo;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

class JitterTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void retryDelayIsTheBaseDelayPlusUpToHalfOfIt() {
        var base = Duration.ofMinutes(10);
        for (int i = 0; i < 1_000; i++) {
            var next = Jitter.nextAttempt(base, NOW);
            assertThat(next).isBetween(NOW.plus(base), NOW.plus(Duration.ofMinutes(15)));
        }
    }

    @Test
    void bulkSubmissionsAreSpreadAcrossTheWindowInsteadOfAllAtOnce() {
        var window = Duration.ofMinutes(10);
        var seen = new HashSet<Instant>();
        for (int i = 0; i < 1_000; i++) {
            var at = Jitter.spreadOver(window, NOW);
            assertThat(at).isBetween(NOW, NOW.plus(window));
            seen.add(at);
        }
        assertThat(seen).hasSizeGreaterThan(900);
    }
}
