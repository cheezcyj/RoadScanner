package com.roadscanner.service.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpSession;

import com.roadscanner.service.user.EmailVerificationService.Purpose;
import com.roadscanner.service.user.EmailVerificationService.RateLimitExceededException;
import com.roadscanner.service.user.EmailVerificationService.VerificationOutcome;
import com.roadscanner.service.user.EmailVerificationService.VerificationResult;

public class EmailVerificationServiceTest {

    private MutableClock clock;
    private EmailVerificationService service;
    private MockHttpSession session;

    @Before
    public void setUp() {
        clock = new MutableClock(Instant.parse("2026-08-01T00:00:00Z"));
        service = new EmailVerificationService(new PredictableSecureRandom(), clock);
        session = new MockHttpSession();
    }

    @Test
    public void challengeUsesSixDigitsAndGrantIsBoundToEmailAndPurpose() {
        String code = service.createChallenge(session, " member@Example.com ", Purpose.REGISTRATION);

        assertTrue(code.matches("\\d{6}"));
        VerificationOutcome verified = service.verify(
                session,
                "member@example.com",
                Purpose.REGISTRATION,
                code);
        assertEquals(VerificationResult.VERIFIED, verified.getResult());
        assertTrue(verified.getProofToken().length() >= 43);
        assertFalse(service.consume(
                session, "other@example.com", Purpose.REGISTRATION, verified.getProofToken()));
        assertFalse(service.consume(
                session, "Member@example.com", Purpose.REGISTRATION, verified.getProofToken()));
        assertFalse(service.consume(
                session, "member@example.com", Purpose.PASSWORD_RESET, verified.getProofToken()));
        assertFalse(service.consume(session, "member@example.com", Purpose.REGISTRATION, null));
        assertFalse(service.consume(session, "member@example.com", Purpose.REGISTRATION, "wrong-proof"));
        String wrongProof = verified.getProofToken().substring(0, 42)
                + (verified.getProofToken().endsWith("A") ? "B" : "A");
        assertFalse(service.consume(
                session, "member@example.com", Purpose.REGISTRATION, wrongProof));
        assertTrue(service.consume(
                session, "member@EXAMPLE.COM", Purpose.REGISTRATION, verified.getProofToken()));
        assertFalse(service.consume(
                session, "member@example.com", Purpose.REGISTRATION, verified.getProofToken()));
    }

    @Test
    public void wrongCodeIsLockedAfterMaximumAttempts() {
        String code = service.createChallenge(session, "member@example.com", Purpose.PASSWORD_RESET);
        String wrongCode = "000000".equals(code) ? "000001" : "000000";

        for (int attempt = 1; attempt < EmailVerificationService.MAX_ATTEMPTS; attempt++) {
            assertEquals(
                    VerificationResult.INVALID,
                    service.verify(
                            session,
                            "member@example.com",
                            Purpose.PASSWORD_RESET,
                            wrongCode).getResult());
        }
        assertEquals(
                VerificationResult.LOCKED,
                service.verify(
                        session,
                        "member@example.com",
                        Purpose.PASSWORD_RESET,
                        wrongCode).getResult());
        assertEquals(
                VerificationResult.INVALID,
                service.verify(
                        session,
                        "member@example.com",
                        Purpose.PASSWORD_RESET,
                        code).getResult());
    }

    @Test
    public void challengeAndVerifiedGrantExpire() {
        String challengeCode = service.createChallenge(
                session,
                "member@example.com",
                Purpose.REGISTRATION);
        clock.advance(EmailVerificationService.CHALLENGE_TTL.plusMillis(1));

        assertEquals(
                VerificationResult.EXPIRED,
                service.verify(
                        session,
                        "member@example.com",
                        Purpose.REGISTRATION,
                        challengeCode).getResult());

        String grantCode = service.createChallenge(session, "member@example.com", Purpose.REGISTRATION);
        VerificationOutcome verified = service.verify(
                session,
                "member@example.com",
                Purpose.REGISTRATION,
                grantCode);
        assertEquals(VerificationResult.VERIFIED, verified.getResult());
        clock.advance(EmailVerificationService.VERIFIED_GRANT_TTL.plusMillis(1));

        assertFalse(service.consume(
                session,
                "member@example.com",
                Purpose.REGISTRATION,
                verified.getProofToken()));
    }

    @Test
    public void resendingInvalidatesEarlierCodeAndVerifiedGrant() {
        String firstCode = service.createChallenge(session, "member@example.com", Purpose.REGISTRATION);
        clock.advance(EmailVerificationService.SEND_COOLDOWN);
        String secondCode = service.createChallenge(session, "member@example.com", Purpose.REGISTRATION);

        assertEquals(
                VerificationResult.INVALID,
                service.verify(
                        session,
                        "member@example.com",
                        Purpose.REGISTRATION,
                        firstCode).getResult());
        VerificationOutcome verified = service.verify(
                session,
                "member@example.com",
                Purpose.REGISTRATION,
                secondCode);
        assertEquals(VerificationResult.VERIFIED, verified.getResult());

        clock.advance(EmailVerificationService.SEND_COOLDOWN);
        service.createChallenge(session, "member@example.com", Purpose.REGISTRATION);
        assertFalse(service.consume(
                session,
                "member@example.com",
                Purpose.REGISTRATION,
                verified.getProofToken()));
    }

    @Test
    public void sendCooldownAndHourlyLimitPreventRepeatedIssuance() {
        service.createChallenge(session, "member@example.com", Purpose.REGISTRATION);

        assertRateLimited(Purpose.REGISTRATION);

        for (int send = 1; send < EmailVerificationService.MAX_SENDS_PER_WINDOW; send++) {
            clock.advance(EmailVerificationService.SEND_COOLDOWN);
            service.createChallenge(session, "member@example.com", Purpose.REGISTRATION);
        }

        clock.advance(EmailVerificationService.SEND_COOLDOWN);
        assertRateLimited(Purpose.REGISTRATION);

        clock.advance(EmailVerificationService.SEND_WINDOW);
        assertTrue(service.createChallenge(
                session,
                "member@example.com",
                Purpose.REGISTRATION).matches("\\d{6}"));
    }

	@Test
	public void replacingSessionDoesNotResetEmailCooldown() {
		service.createChallenge(
				new MockHttpSession(),
				"member@example.com",
				Purpose.REGISTRATION,
				"192.0.2.10");

		try {
			service.createChallenge(
					new MockHttpSession(),
					"member@example.com",
					Purpose.REGISTRATION,
					"192.0.2.10");
			org.junit.Assert.fail("Expected a new session to remain rate limited");
		} catch (RateLimitExceededException expected) {
			// expected
		}
	}

	@Test
	public void addressLimitAppliesAcrossEmailsAndSessions() {
		for (int send = 0; send < EmailVerificationService.MAX_SENDS_PER_ADDRESS_WINDOW; send++) {
			service.createChallenge(
					new MockHttpSession(),
					"member" + send + "@example.com",
					Purpose.REGISTRATION,
					"192.0.2.20");
		}

		try {
			service.createChallenge(
					new MockHttpSession(),
					"blocked@example.com",
					Purpose.REGISTRATION,
					"192.0.2.20");
			org.junit.Assert.fail("Expected address send limit");
		} catch (RateLimitExceededException expected) {
			// expected
		}
	}

    @Test
    public void failedOlderDeliveryCannotClearNewerChallenge() {
        String firstCode = service.createChallenge(session, "member@example.com", Purpose.PASSWORD_RESET);
        clock.advance(EmailVerificationService.SEND_COOLDOWN);
        String secondCode = service.createChallenge(session, "member@example.com", Purpose.PASSWORD_RESET);

        service.clearChallenge(
                session,
                "member@example.com",
                Purpose.PASSWORD_RESET,
                firstCode);

        assertEquals(
                VerificationResult.VERIFIED,
                service.verify(
                        session,
                        "member@example.com",
                        Purpose.PASSWORD_RESET,
                        secondCode).getResult());
    }

    @Test
    public void concurrentConsumersCanUseGrantOnlyOnce() throws Exception {
        String code = service.createChallenge(session, "member@example.com", Purpose.REGISTRATION);
        VerificationOutcome verified = service.verify(
                session,
                "member@example.com",
                Purpose.REGISTRATION,
                code);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<Boolean> consume = () -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            return service.consume(
                    session,
                    "member@example.com",
                    Purpose.REGISTRATION,
                    verified.getProofToken());
        };

        try {
            Future<Boolean> first = executor.submit(consume);
            Future<Boolean> second = executor.submit(consume);
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            assertTrue(first.get(5, TimeUnit.SECONDS) ^ second.get(5, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private void assertRateLimited(Purpose purpose) {
        try {
            service.createChallenge(session, "member@example.com", purpose);
            org.junit.Assert.fail("Expected email verification send to be rate limited");
        } catch (RateLimitExceededException expected) {
            // expected
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advance(java.time.Duration duration) {
            instant = instant.plus(duration);
        }
    }

    private static final class PredictableSecureRandom extends SecureRandom {
        private static final long serialVersionUID = 1L;
        private int nextCode = 123456;

        @Override
        public int nextInt(int bound) {
            int result = nextCode % bound;
            nextCode = (nextCode + 111111) % bound;
            return result;
        }

        @Override
        public void nextBytes(byte[] bytes) {
            for (int index = 0; index < bytes.length; index++) {
                bytes[index] = (byte) (index + nextCode);
            }
        }
    }
}
