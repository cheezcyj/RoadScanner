package com.roadscanner.service.user;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Component;

/**
 * Manages short-lived email verification challenges in the user's server-side
 * session. Verification grants are scoped to an email and a purpose and can be
 * consumed only once.
 */
@Component
public class EmailVerificationService {

    public enum Purpose {
        REGISTRATION,
        PASSWORD_RESET,
        ID_RECOVERY
    }

    public enum VerificationResult {
        VERIFIED,
        INVALID,
        EXPIRED,
        LOCKED
    }

    public static final class VerificationOutcome {
        private final VerificationResult result;
        private final String proofToken;

        private VerificationOutcome(VerificationResult result, String proofToken) {
            this.result = result;
            this.proofToken = proofToken;
        }

        public VerificationResult getResult() {
            return result;
        }

        public String getProofToken() {
            return proofToken;
        }
    }

    public static final class RateLimitExceededException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private RateLimitExceededException() {
            super("email verification send limit exceeded");
        }
    }

    static final int MAX_ATTEMPTS = 5;
    static final Duration CHALLENGE_TTL = Duration.ofMinutes(10);
    static final Duration VERIFIED_GRANT_TTL = Duration.ofMinutes(10);
    static final Duration SEND_COOLDOWN = Duration.ofSeconds(30);
    static final Duration SEND_WINDOW = Duration.ofHours(1);
    static final int MAX_SENDS_PER_WINDOW = 5;
    static final int MAX_SENDS_PER_ADDRESS_WINDOW = 20;
    static final int MAX_TRACKED_RATE_LIMIT_KEYS = 10_000;

    private static final String SESSION_PREFIX = EmailVerificationService.class.getName();
    private static final int CODE_BOUND = 1_000_000;
    private static final int SALT_LENGTH = 16;
    private static final int PROOF_TOKEN_LENGTH = 32;

    private final SecureRandom secureRandom;
    private final Clock clock;
    private final Object sharedIssuanceLock = new Object();
    private final Map<String, SharedIssuanceState> sharedIssuanceStates = new HashMap<>();

    public EmailVerificationService() {
        this(new SecureRandom(), Clock.systemUTC());
    }

    EmailVerificationService(SecureRandom secureRandom, Clock clock) {
        if (secureRandom == null || clock == null) {
            throw new IllegalArgumentException("secureRandom and clock are required");
        }
        this.secureRandom = secureRandom;
        this.clock = clock;
    }

    /**
     * Creates a new challenge, invalidating any earlier challenge or grant for
     * the same purpose. The returned code is for mail delivery only and must not
     * be included in an HTTP response.
     */
    public String createChallenge(HttpSession session, String email, Purpose purpose) {
        return createChallenge(session, email, purpose, null);
    }

    /**
     * Creates a challenge with process-wide limits keyed independently by the
     * normalized email and the direct peer address. The session still owns the
     * challenge itself, but replacing a cookie cannot reset these send limits.
     */
    public String createChallenge(
            HttpSession session,
            String email,
            Purpose purpose,
            String clientAddress) {
        requireSessionAndPurpose(session, purpose);
        String normalizedEmail = normalizeEmail(email);
        Instant now = clock.instant();
        reserveIssuance(session, normalizedEmail, purpose, clientAddress, now);
        synchronized (session) {
            String code = String.format(Locale.ROOT, "%06d", secureRandom.nextInt(CODE_BOUND));
            byte[] salt = new byte[SALT_LENGTH];
            secureRandom.nextBytes(salt);
            Challenge challenge = new Challenge(
                    normalizedEmail,
                    purpose,
                    now.plus(CHALLENGE_TTL),
                    MAX_ATTEMPTS,
                    salt,
                    hashCode(normalizedEmail, purpose, code, salt));
            session.setAttribute(pendingKey(purpose), challenge);
            session.removeAttribute(grantKey(purpose));
            return code;
        }
    }

    /** Reserves a rate-limited outbound message that does not require a code. */
    public void reserveSend(
            HttpSession session,
            String email,
            Purpose purpose,
            String clientAddress) {
        requireSessionAndPurpose(session, purpose);
        String normalizedEmail = normalizeEmail(email);
        reserveIssuance(session, normalizedEmail, purpose, clientAddress, clock.instant());
    }

    private void reserveIssuance(
            HttpSession session,
            String normalizedEmail,
            Purpose purpose,
            String clientAddress,
            Instant now) {
        reserveSharedIssuance(normalizedEmail, purpose, clientAddress, now);
        synchronized (session) {
            IssuanceState issuanceState = issuanceState(session, purpose);
            if (issuanceState != null
                    && now.isBefore(issuanceState.lastIssuedAt.plus(SEND_COOLDOWN))) {
                throw new RateLimitExceededException();
            }

            boolean currentWindow = issuanceState != null
                    && now.isBefore(issuanceState.windowStartedAt.plus(SEND_WINDOW));
            if (currentWindow && issuanceState.sendCount >= MAX_SENDS_PER_WINDOW) {
                throw new RateLimitExceededException();
            }

            IssuanceState nextIssuanceState = currentWindow
                    ? new IssuanceState(
                            purpose,
                            issuanceState.windowStartedAt,
                            now,
                            issuanceState.sendCount + 1)
                    : new IssuanceState(purpose, now, now, 1);
            session.setAttribute(issuanceKey(purpose), nextIssuanceState);
        }
    }

    private void reserveSharedIssuance(
            String normalizedEmail,
            Purpose purpose,
            String clientAddress,
            Instant now) {
        String emailKey = sharedKey("email", normalizedEmail, purpose);
        String normalizedAddress = normalizeClientAddress(clientAddress);
        String addressKey = normalizedAddress == null
                ? null
                : sharedKey("address", normalizedAddress, purpose);

        synchronized (sharedIssuanceLock) {
            removeExpiredSharedIssuance(now);
            SharedIssuanceState emailState = sharedIssuanceStates.get(emailKey);
            SharedIssuanceState addressState = addressKey == null
                    ? null
                    : sharedIssuanceStates.get(addressKey);
            assertSharedLimit(emailState, now, MAX_SENDS_PER_WINDOW, true);
            assertSharedLimit(addressState, now, MAX_SENDS_PER_ADDRESS_WINDOW, false);

            int newKeys = (emailState == null ? 1 : 0)
                    + (addressKey != null && addressState == null ? 1 : 0);
            if (sharedIssuanceStates.size() + newKeys > MAX_TRACKED_RATE_LIMIT_KEYS) {
                throw new RateLimitExceededException();
            }
            sharedIssuanceStates.put(emailKey, nextSharedState(emailState, now));
            if (addressKey != null) {
                sharedIssuanceStates.put(addressKey, nextSharedState(addressState, now));
            }
        }
    }

    private void assertSharedLimit(
            SharedIssuanceState state,
            Instant now,
            int maximumSends,
            boolean enforceCooldown) {
        if (state == null) {
            return;
        }
        if (enforceCooldown && now.isBefore(state.lastIssuedAt.plus(SEND_COOLDOWN))) {
            throw new RateLimitExceededException();
        }
        if (now.isBefore(state.windowStartedAt.plus(SEND_WINDOW))
                && state.sendCount >= maximumSends) {
            throw new RateLimitExceededException();
        }
    }

    private SharedIssuanceState nextSharedState(SharedIssuanceState state, Instant now) {
        if (state != null && now.isBefore(state.windowStartedAt.plus(SEND_WINDOW))) {
            return new SharedIssuanceState(
                    state.windowStartedAt, now, state.sendCount + 1);
        }
        return new SharedIssuanceState(now, now, 1);
    }

    private void removeExpiredSharedIssuance(Instant now) {
        sharedIssuanceStates.entrySet().removeIf(entry ->
                !now.isBefore(entry.getValue().windowStartedAt.plus(SEND_WINDOW)));
    }

    private String sharedKey(String kind, String identity, Purpose purpose) {
        return kind + ':' + purpose.name() + ':' + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(digest(identity.getBytes(StandardCharsets.UTF_8)));
    }

    private String normalizeClientAddress(String clientAddress) {
        if (clientAddress == null) {
            return null;
        }
        String normalized = clientAddress.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.length() > 64 ? normalized.substring(0, 64) : normalized;
    }

    public VerificationOutcome verify(HttpSession session, String email, Purpose purpose, String submittedCode) {
        requireSessionAndPurpose(session, purpose);
        synchronized (session) {
            Challenge challenge = challenge(session, purpose);
            if (challenge == null) {
                return outcome(VerificationResult.INVALID);
            }

            if (!purpose.equals(challenge.purpose)) {
                clear(session, purpose);
                return outcome(VerificationResult.INVALID);
            }

            if (!clock.instant().isBefore(challenge.expiresAt)) {
                session.removeAttribute(pendingKey(purpose));
                return outcome(VerificationResult.EXPIRED);
            }

            String normalizedEmail;
            try {
                normalizedEmail = normalizeEmail(email);
            } catch (IllegalArgumentException exception) {
                return failedAttempt(session, purpose, challenge);
            }

            boolean emailMatches = challenge.email.equals(normalizedEmail);
            boolean codeMatches = submittedCode != null
                    && submittedCode.matches("\\d{6}")
                    && MessageDigest.isEqual(
                            challenge.codeHash,
                            hashCode(normalizedEmail, purpose, submittedCode, challenge.salt));

            if (!emailMatches || !codeMatches) {
                return failedAttempt(session, purpose, challenge);
            }

            byte[] proofBytes = new byte[PROOF_TOKEN_LENGTH];
            secureRandom.nextBytes(proofBytes);
            String proofToken = Base64.getUrlEncoder().withoutPadding().encodeToString(proofBytes);
            session.removeAttribute(pendingKey(purpose));
            session.setAttribute(
                    grantKey(purpose),
                    new VerifiedGrant(
                            normalizedEmail,
                            purpose,
                            clock.instant().plus(VERIFIED_GRANT_TTL),
                            digest(proofToken.getBytes(StandardCharsets.UTF_8))));
            return new VerificationOutcome(VerificationResult.VERIFIED, proofToken);
        }
    }

    /**
     * Atomically checks and removes a verified grant. A successful grant is
     * therefore valid for exactly one sensitive operation.
     */
    public boolean consume(HttpSession session, String email, Purpose purpose, String proofToken) {
        requireSessionAndPurpose(session, purpose);
        synchronized (session) {
            Object value = session.getAttribute(grantKey(purpose));
            if (!(value instanceof VerifiedGrant)) {
                return false;
            }

            VerifiedGrant grant = (VerifiedGrant) value;
            if (!purpose.equals(grant.purpose) || !clock.instant().isBefore(grant.expiresAt)) {
                session.removeAttribute(grantKey(purpose));
                return false;
            }

            String normalizedEmail;
            try {
                normalizedEmail = normalizeEmail(email);
            } catch (IllegalArgumentException exception) {
                return false;
            }

            if (!grant.email.equals(normalizedEmail)) {
                return false;
            }

            if (proofToken == null
                    || proofToken.length() != 43
                    || !MessageDigest.isEqual(
                    grant.proofHash,
                    digest(proofToken.getBytes(StandardCharsets.UTF_8)))) {
                return false;
            }

            session.removeAttribute(grantKey(purpose));
            return true;
        }
    }

    public void clear(HttpSession session, Purpose purpose) {
        requireSessionAndPurpose(session, purpose);
        synchronized (session) {
            session.removeAttribute(pendingKey(purpose));
            session.removeAttribute(grantKey(purpose));
        }
    }

    /** Clears only the still-current challenge associated with a failed mail send. */
    public void clearChallenge(HttpSession session, String email, Purpose purpose, String code) {
        requireSessionAndPurpose(session, purpose);
        String normalizedEmail;
        try {
            normalizedEmail = normalizeEmail(email);
        } catch (IllegalArgumentException exception) {
            return;
        }

        synchronized (session) {
            Challenge current = challenge(session, purpose);
            if (current == null || code == null || !current.email.equals(normalizedEmail)) {
                return;
            }

            byte[] submittedHash = hashCode(normalizedEmail, purpose, code, current.salt);
            if (MessageDigest.isEqual(current.codeHash, submittedHash)) {
                session.removeAttribute(pendingKey(purpose));
            }
        }
    }

    public String normalizeEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("email is required");
        }

        String trimmed = email.trim();
        int at = trimmed.indexOf('@');
        if (trimmed.length() > 254
                || containsWhitespace(trimmed)
                || at <= 0
                || at != trimmed.lastIndexOf('@')
                || at == trimmed.length() - 1
                || trimmed.indexOf('.', at) < 0) {
            throw new IllegalArgumentException("invalid email address");
        }

        String localPart = trimmed.substring(0, at);
        String domain = trimmed.substring(at + 1).toLowerCase(Locale.ROOT);
        return localPart + "@" + domain;
    }

    private VerificationOutcome failedAttempt(
            HttpSession session,
            Purpose purpose,
            Challenge challenge) {
        int attemptsRemaining = challenge.attemptsRemaining - 1;
        if (attemptsRemaining <= 0) {
            session.removeAttribute(pendingKey(purpose));
            return outcome(VerificationResult.LOCKED);
        }

        session.setAttribute(pendingKey(purpose), challenge.withAttemptsRemaining(attemptsRemaining));
        return outcome(VerificationResult.INVALID);
    }

    private Challenge challenge(HttpSession session, Purpose purpose) {
        Object value = session.getAttribute(pendingKey(purpose));
        return value instanceof Challenge ? (Challenge) value : null;
    }

    private IssuanceState issuanceState(HttpSession session, Purpose purpose) {
        Object value = session.getAttribute(issuanceKey(purpose));
        if (!(value instanceof IssuanceState)) {
            return null;
        }
        IssuanceState state = (IssuanceState) value;
        return purpose.equals(state.purpose) ? state : null;
    }

    private byte[] hashCode(String email, Purpose purpose, String code, byte[] salt) {
        MessageDigest digest = sha256();
        digest.update(salt);
        digest.update(email.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) ':');
        digest.update(purpose.name().getBytes(StandardCharsets.UTF_8));
        digest.update((byte) ':');
        digest.update(code.getBytes(StandardCharsets.UTF_8));
        return digest.digest();
    }

    private byte[] digest(byte[] value) {
        return sha256().digest(value);
    }

    private MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static VerificationOutcome outcome(VerificationResult result) {
        return new VerificationOutcome(result, null);
    }

    private static void requireSessionAndPurpose(HttpSession session, Purpose purpose) {
        if (session == null || purpose == null) {
            throw new IllegalArgumentException("session and purpose are required");
        }
    }

    private static boolean containsWhitespace(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isWhitespace(value.charAt(index))) {
                return true;
            }
        }
        return false;
    }

    private static String pendingKey(Purpose purpose) {
        return SESSION_PREFIX + ".pending." + purpose.name();
    }

    private static String grantKey(Purpose purpose) {
        return SESSION_PREFIX + ".grant." + purpose.name();
    }

    private static String issuanceKey(Purpose purpose) {
        return SESSION_PREFIX + ".issuance." + purpose.name();
    }

    private static final class Challenge implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String email;
        private final Purpose purpose;
        private final Instant expiresAt;
        private final int attemptsRemaining;
        private final byte[] salt;
        private final byte[] codeHash;

        private Challenge(
                String email,
                Purpose purpose,
                Instant expiresAt,
                int attemptsRemaining,
                byte[] salt,
                byte[] codeHash) {
            this.email = email;
            this.purpose = purpose;
            this.expiresAt = expiresAt;
            this.attemptsRemaining = attemptsRemaining;
            this.salt = salt.clone();
            this.codeHash = codeHash.clone();
        }

        private Challenge withAttemptsRemaining(int value) {
            return new Challenge(email, purpose, expiresAt, value, salt, codeHash);
        }
    }

    private static final class VerifiedGrant implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String email;
        private final Purpose purpose;
        private final Instant expiresAt;
        private final byte[] proofHash;

        private VerifiedGrant(String email, Purpose purpose, Instant expiresAt, byte[] proofHash) {
            this.email = email;
            this.purpose = purpose;
            this.expiresAt = expiresAt;
            this.proofHash = proofHash.clone();
        }
    }

    private static final class IssuanceState implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Purpose purpose;
        private final Instant windowStartedAt;
        private final Instant lastIssuedAt;
        private final int sendCount;

        private IssuanceState(
                Purpose purpose,
                Instant windowStartedAt,
                Instant lastIssuedAt,
                int sendCount) {
            this.purpose = purpose;
            this.windowStartedAt = windowStartedAt;
            this.lastIssuedAt = lastIssuedAt;
            this.sendCount = sendCount;
        }
    }

    private static final class SharedIssuanceState {
        private final Instant windowStartedAt;
        private final Instant lastIssuedAt;
        private final int sendCount;

        private SharedIssuanceState(
                Instant windowStartedAt,
                Instant lastIssuedAt,
                int sendCount) {
            this.windowStartedAt = windowStartedAt;
            this.lastIssuedAt = lastIssuedAt;
            this.sendCount = sendCount;
        }
    }
}
