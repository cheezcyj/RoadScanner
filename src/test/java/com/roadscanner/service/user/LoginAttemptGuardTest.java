package com.roadscanner.service.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.LongSupplier;

import org.junit.Test;

public class LoginAttemptGuardTest {

    @Test
    public void accountLimitAppliesAcrossDifferentClientAddresses() {
        MutableTime time = new MutableTime(1_000L);
        LoginAttemptGuard guard = guard(time);

        guard.recordFailure("Member01", "192.0.2.1");
        guard.recordFailure("member01", "192.0.2.2");
        guard.recordFailure(" member01 ", "192.0.2.3");

        assertThat(guard.isBlocked("MEMBER01", "192.0.2.99")).isTrue();
    }

    @Test
    public void addressLimitAppliesAcrossDifferentAccounts() {
        MutableTime time = new MutableTime(1_000L);
        LoginAttemptGuard guard = guard(time);

        guard.recordFailure("member01", "192.0.2.10");
        guard.recordFailure("member02", "192.0.2.10");
        guard.recordFailure("member03", "192.0.2.10");

        assertThat(guard.isBlocked("member99", "192.0.2.10")).isTrue();
    }

    @Test
    public void temporaryLockExpires() {
        MutableTime time = new MutableTime(1_000L);
        LoginAttemptGuard guard = guard(time);
        failThreeTimes(guard, "member01", "192.0.2.10");

        assertThat(guard.isBlocked("member01", "192.0.2.10")).isTrue();
        time.advance(499L);
        assertThat(guard.isBlocked("member01", "192.0.2.10")).isTrue();
        time.advance(1L);
        assertThat(guard.isBlocked("member01", "192.0.2.10")).isFalse();
    }

    @Test
    public void successfulLoginClearsOnlyAccountFailures() {
        MutableTime time = new MutableTime(1_000L);
        LoginAttemptGuard guard = guard(time);
        guard.recordFailure("member01", "192.0.2.1");
        guard.recordFailure("member01", "192.0.2.2");
        guard.recordFailure("member01", "192.0.2.3");
        assertThat(guard.isBlocked("member01", "192.0.2.99")).isTrue();

        guard.recordSuccess("member01");

        assertThat(guard.isBlocked("member01", "192.0.2.99")).isFalse();
    }

    @Test
    public void successfulLoginDoesNotClearAddressFailures() {
        MutableTime time = new MutableTime(1_000L);
        LoginAttemptGuard guard = guard(time);
        guard.recordFailure("member01", "192.0.2.10");
        guard.recordFailure("member02", "192.0.2.10");
        guard.recordFailure("member03", "192.0.2.10");

        guard.recordSuccess("member01");

        assertThat(guard.isBlocked("member99", "192.0.2.10")).isTrue();
    }

    @Test
    public void trackedStateNeverExceedsConfiguredBound() {
        MutableTime time = new MutableTime(1_000L);
        LoginAttemptGuard guard = new LoginAttemptGuard(3, 3, 1_000L, 500L, 4, time);

        for (int index = 0; index < 20; index++) {
            guard.recordFailure("member" + index, "192.0.2." + index);
            assertThat(guard.trackedKeyCount()).isLessThanOrEqualTo(4);
        }
    }

    @Test
    public void failuresOutsideObservationWindowDoNotAccumulate() {
        MutableTime time = new MutableTime(1_000L);
        LoginAttemptGuard guard = guard(time);
        guard.recordFailure("member01", "192.0.2.10");
        guard.recordFailure("member01", "192.0.2.10");

        time.advance(1_000L);
        guard.recordFailure("member01", "192.0.2.10");

        assertThat(guard.isBlocked("member01", "192.0.2.10")).isFalse();
    }

    private LoginAttemptGuard guard(LongSupplier time) {
        return new LoginAttemptGuard(3, 3, 1_000L, 500L, 100, time);
    }

    private void failThreeTimes(LoginAttemptGuard guard, String accountId, String address) {
        guard.recordFailure(accountId, address);
        guard.recordFailure(accountId, address);
        guard.recordFailure(accountId, address);
    }

    private static final class MutableTime implements LongSupplier {
        private long value;

        private MutableTime(long value) {
            this.value = value;
        }

        @Override
        public long getAsLong() {
            return value;
        }

        private void advance(long millis) {
            value += millis;
        }
    }
}
