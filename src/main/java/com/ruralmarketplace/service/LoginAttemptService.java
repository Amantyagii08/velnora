package com.ruralmarketplace.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Service;
import com.ruralmarketplace.exception.LoginRateLimitException;

@Service
public class LoginAttemptService {
    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final int MAX_TRACKED_KEYS = 10_000;
    private static final int MAX_REGISTRATIONS_PER_HOUR = 10;
    private static final Duration REGISTRATION_WINDOW = Duration.ofHours(1);
    private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();
    private final Map<String, RegistrationState> registrations = new LinkedHashMap<>();

    public synchronized void checkRegistrationAllowedAndRecord(String clientAddress) {
        removeExpired();
        RegistrationState state = registrations.get(clientAddress);
        Instant now = Instant.now();
        if (state == null || state.windowStartedAt.plus(REGISTRATION_WINDOW).isBefore(now)) {
            if (registrations.size() >= MAX_TRACKED_KEYS) {
                registrations.remove(registrations.keySet().iterator().next());
            }
            state = new RegistrationState(now, 0);
        }
        if (state.count >= MAX_REGISTRATIONS_PER_HOUR) {
            throw new LoginRateLimitException("Too many registration attempts. Try again later.");
        }
        state.count++;
        registrations.put(clientAddress, state);
    }

    public synchronized void checkAllowed(String key) {
        removeExpired();
        AttemptState state = attempts.get(key);
        if (state == null && attempts.size() >= MAX_TRACKED_KEYS) {
            throw new LoginRateLimitException("Login protection is temporarily at capacity. Try again later.");
        }
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
        registrations.entrySet().removeIf(entry ->
                entry.getValue().windowStartedAt.plus(REGISTRATION_WINDOW).isBefore(now));
    }

    private static class AttemptState {
        private final Instant windowStartedAt;
        private int failures;
        private Instant lockedUntil;
        private AttemptState(Instant windowStartedAt, int failures, Instant lockedUntil) {
            this.windowStartedAt = windowStartedAt; this.failures = failures; this.lockedUntil = lockedUntil;
        }
    }

    private static class RegistrationState {
        private final Instant windowStartedAt;
        private int count;
        private RegistrationState(Instant windowStartedAt, int count) {
            this.windowStartedAt = windowStartedAt;
            this.count = count;
        }
    }
}
