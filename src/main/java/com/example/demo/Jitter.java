package com.example.demo;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

/** Randomised delays so tasks that failed together don't retry together, and bulk work arrives as a flat load. */
public final class Jitter {

    private Jitter() {
    }

    /** base + up to 50% of base */
    public static Instant nextAttempt(Duration baseDelay, Instant now) {
        long jitterMillis = ThreadLocalRandom.current().nextLong(baseDelay.toMillis() / 2 + 1);
        return now.plus(baseDelay).plusMillis(jitterMillis);
    }

    /** a random moment within [now, now + window) */
    public static Instant spreadOver(Duration window, Instant now) {
        return now.plusMillis(ThreadLocalRandom.current().nextLong(window.toMillis()));
    }
}
