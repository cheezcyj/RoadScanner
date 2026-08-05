package com.roadscanner.service.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.LongSupplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Applies a bounded, process-local login failure limit by account and client IP.
 * Raw identifiers are not retained in the in-memory state.
 */
@Component
public class LoginAttemptGuard {

    private static final int DEFAULT_MAX_ACCOUNT_FAILURES = 5;
    private static final int DEFAULT_MAX_ADDRESS_FAILURES = 25;
    private static final long DEFAULT_FAILURE_WINDOW_MILLIS = 10 * 60_000L;
    private static final long DEFAULT_LOCK_MILLIS = 15 * 60_000L;
    private static final int DEFAULT_MAX_TRACKED_KEYS = 10_000;

    private final Object monitor = new Object();
    private final LinkedHashMap<String, AttemptState> attempts =
            new LinkedHashMap<String, AttemptState>(16, 0.75f, true);
    private final int maxAccountFailures;
    private final int maxAddressFailures;
    private final long failureWindowMillis;
    private final long lockMillis;
    private final int maxTrackedKeys;
    private final LongSupplier currentTimeMillis;

    public LoginAttemptGuard() {
        this(DEFAULT_MAX_ACCOUNT_FAILURES,
                DEFAULT_MAX_ADDRESS_FAILURES,
                DEFAULT_FAILURE_WINDOW_MILLIS,
                DEFAULT_LOCK_MILLIS,
                DEFAULT_MAX_TRACKED_KEYS,
                System::currentTimeMillis);
    }

    @Autowired
    public LoginAttemptGuard(
            @Value("${roadscanner.security.login.max-failures:5}") int maxFailures,
            @Value("${roadscanner.security.login.ip-max-failures:25}") int maxAddressFailures,
            @Value("${roadscanner.security.login.failure-window-millis:600000}") long failureWindowMillis,
            @Value("${roadscanner.security.login.lock-millis:900000}") long lockMillis,
            @Value("${roadscanner.security.login.max-tracked-keys:10000}") int maxTrackedKeys) {
        this(maxFailures,
                maxAddressFailures,
                failureWindowMillis,
                lockMillis,
                maxTrackedKeys,
                System::currentTimeMillis);
    }

    LoginAttemptGuard(
            int maxFailures,
            int maxAddressFailures,
            long failureWindowMillis,
            long lockMillis,
            int maxTrackedKeys,
            LongSupplier currentTimeMillis) {
        if (maxFailures < 1 || maxAddressFailures < 1 || failureWindowMillis < 1
                || lockMillis < 1 || maxTrackedKeys < 2) {
            throw new IllegalArgumentException("Login attempt limit settings must be positive");
        }
        if (currentTimeMillis == null) {
            throw new IllegalArgumentException("Current time supplier is required");
        }
        this.maxAccountFailures = maxFailures;
        this.maxAddressFailures = maxAddressFailures;
        this.failureWindowMillis = failureWindowMillis;
        this.lockMillis = lockMillis;
        this.maxTrackedKeys = maxTrackedKeys;
        this.currentTimeMillis = currentTimeMillis;
    }

    public boolean isBlocked(String accountId, String clientAddress) {
        long now = currentTimeMillis.getAsLong();
        String accountKey = accountKey(accountId);
        String addressKey = addressKey(clientAddress);
        synchronized (monitor) {
            return isKeyBlocked(accountKey, now) || isKeyBlocked(addressKey, now);
        }
    }

    public void recordFailure(String accountId, String clientAddress) {
        long now = currentTimeMillis.getAsLong();
        synchronized (monitor) {
            addFailure(accountKey(accountId), now, maxAccountFailures);
            addFailure(addressKey(clientAddress), now, maxAddressFailures);
        }
    }

    public void recordSuccess(String accountId) {
        synchronized (monitor) {
            attempts.remove(accountKey(accountId));
        }
    }

    int trackedKeyCount() {
        synchronized (monitor) {
            return attempts.size();
        }
    }

    private boolean isKeyBlocked(String key, long now) {
        AttemptState state = attempts.get(key);
        if (state == null) {
            return false;
        }
        if (state.lockUntilMillis > now) {
            return true;
        }
        if (state.lockUntilMillis > 0
                || elapsedAtLeast(now, state.firstFailureMillis, failureWindowMillis)) {
            attempts.remove(key);
        }
        return false;
    }

    private void addFailure(String key, long now, int failureLimit) {
        AttemptState state = attempts.get(key);
        if (state == null
                || state.lockUntilMillis > 0 && state.lockUntilMillis <= now
                || elapsedAtLeast(now, state.firstFailureMillis, failureWindowMillis)) {
            state = new AttemptState(now);
            putBounded(key, state);
        }
        if (state.lockUntilMillis > now) {
            return;
        }

        state.failures++;
        if (state.failures >= failureLimit) {
            state.lockUntilMillis = safeAdd(now, lockMillis);
        }
    }

    private void putBounded(String key, AttemptState state) {
        if (!attempts.containsKey(key) && attempts.size() >= maxTrackedKeys) {
            Iterator<Map.Entry<String, AttemptState>> iterator = attempts.entrySet().iterator();
            if (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }
        attempts.put(key, state);
    }

    private String accountKey(String accountId) {
        String normalized = accountId == null
                ? ""
                : accountId.trim().toLowerCase(Locale.ROOT);
        return "account:" + sha256(normalized);
    }

    private String addressKey(String clientAddress) {
        String normalized = clientAddress == null ? "" : clientAddress.trim();
        return "address:" + sha256(normalized);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte element : digest) {
                result.append(String.format(Locale.ROOT, "%02x", element & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private boolean elapsedAtLeast(long now, long startedAt, long duration) {
        return now >= startedAt && now - startedAt >= duration;
    }

    private long safeAdd(long value, long increment) {
        return value > Long.MAX_VALUE - increment ? Long.MAX_VALUE : value + increment;
    }

    private static final class AttemptState {
        private final long firstFailureMillis;
        private int failures;
        private long lockUntilMillis;

        private AttemptState(long firstFailureMillis) {
            this.firstFailureMillis = firstFailureMillis;
        }
    }
}
