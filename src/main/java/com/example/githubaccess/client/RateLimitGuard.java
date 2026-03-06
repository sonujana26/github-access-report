package com.example.githubaccess.client;

import com.example.githubaccess.exception.RateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RateLimitGuard {

    private static final Logger log = LoggerFactory.getLogger(RateLimitGuard.class);
    private static final int FAIL_FAST_THRESHOLD_SECONDS = 60;

    private final AtomicInteger remaining = new AtomicInteger(5000);
    private final AtomicLong resetEpochSeconds = new AtomicLong(0);

    public void update(int newRemaining, long newResetEpochSeconds) {
        remaining.set(newRemaining);
        resetEpochSeconds.set(newResetEpochSeconds);
        log.debug("Rate limit: {} requests remaining, resets at {}", newRemaining,
                Instant.ofEpochSecond(newResetEpochSeconds));
    }

    public Mono<Void> checkOrWait() {
        int current = remaining.get();
        if (current > 0) {
            return Mono.empty();
        }

        long reset = resetEpochSeconds.get();
        long waitSeconds = reset - Instant.now().getEpochSecond();

        if (waitSeconds <= 0) {
            return Mono.empty();
        }

        if (waitSeconds > FAIL_FAST_THRESHOLD_SECONDS) {
            log.warn("Rate limit exhausted. Reset is {}s away — failing fast.", waitSeconds);
            throw new RateLimitExceededException(reset);
        }

        log.warn("Rate limit exhausted. Waiting {}s until reset at {}.", waitSeconds, Instant.ofEpochSecond(reset));
        return Mono.delay(Duration.ofSeconds(waitSeconds + 1)).then();
    }
}
