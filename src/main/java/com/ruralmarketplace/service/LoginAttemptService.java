package com.ruralmarketplace.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import com.ruralmarketplace.exception.LoginRateLimitException;

@Service
public class LoginAttemptService {
    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();

    public synchronized void checkAllowed(String key) {
        removeExpired();
        AttemptState state = attempts.get(key);
        if (state != null && state.lockedUntil != null && state.lockedUntil.isAfter(Instant.now())) {
            throw new LoginRateLimitException("Too many failed login attempts. Try again in 15 minutes.");
        }
    }

    public synchronized void recordFailure(String key) {
        Instant now = Instant.now();
        AttemptState state = attempts.get(key);
        if (state == null || state.windowStartedAt.plus(WINDOW).isBefore(now)) {
            state = new AttemptState(now, 0, null);
        }
        state.failures++;
        if (state.failures >= MAX_FAILURES) state.lockedUntil = now.plus(LOCK_DURATION);
        attempts.put(key, state);
        removeExpired();
    }

    public synchronized void recordSuccess(String key) {
        attempts.remove(key);
    }

    private void removeExpired() {
        Instant now = Instant.now();
        attempts.entrySet().removeIf(entry -> {
            AttemptState state = entry.getValue();
            return state.windowStartedAt.plus(WINDOW).isBefore(now)
                    && (state.lockedUntil == null || state.lockedUntil.isBefore(now));
        });
    }

    private static class AttemptState {
        private final Instant windowStartedAt;
        private int failures;
        private Instant lockedUntil;
        private AttemptState(Instant windowStartedAt, int failures, Instant lockedUntil) {
            this.windowStartedAt = windowStartedAt; this.failures = failures; this.lockedUntil = lockedUntil;
        }
    }
}
