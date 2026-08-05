package com.roadscanner.service.user;

import java.io.UnsupportedEncodingException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import javax.mail.MessagingException;

import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

/** Prevents local smoke tests from sending email outside the process. */
@Component
@Profile("local")
public class LocalMailSendService extends MailSendService {
    private static final int MAX_MESSAGES = 20;

    private final Deque<LocalMailMessage> messages = new ArrayDeque<LocalMailMessage>();
    private final Clock clock;

    public LocalMailSendService() {
        this(Clock.systemUTC());
    }

    LocalMailSendService(Clock clock) {
        super(new JavaMailSenderImpl(), "no-reply@example.invalid");
        this.clock = clock;
    }

    @Override
    public void sendRegistrationVerification(String email, String verificationCode)
            throws UnsupportedEncodingException, MessagingException {
        capture("회원가입 인증", email, verificationMessage(verificationCode));
    }

    @Override
    public void sendPasswordResetVerification(String email, String verificationCode)
            throws UnsupportedEncodingException, MessagingException {
        capture("비밀번호 재설정", email, verificationMessage(verificationCode));
    }

    @Override
    public String findId(String email, String id)
            throws UnsupportedEncodingException, MessagingException {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("id is required");
        }
        capture("아이디 찾기", email, "아이디: " + id);
        return email;
    }

    public synchronized List<LocalMailMessage> getMessages() {
        return Collections.unmodifiableList(new ArrayList<LocalMailMessage>(messages));
    }

    public synchronized void clear() {
        messages.clear();
    }

    private String verificationMessage(String verificationCode) {
        if (verificationCode == null || !verificationCode.matches("\\d{6}")) {
            throw new IllegalArgumentException("verification code must contain six digits");
        }
        return "인증번호: " + verificationCode;
    }

    private synchronized void capture(String type, String email, String contents) {
        messages.addFirst(new LocalMailMessage(type, maskEmail(email), contents, clock.instant()));
        while (messages.size() > MAX_MESSAGES) {
            messages.removeLast();
        }
    }

    private String maskEmail(String email) {
        if (email == null || email.indexOf('\r') >= 0 || email.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("invalid email address");
        }
        String trimmed = email.trim();
        int at = trimmed.indexOf('@');
        if (at <= 0 || at != trimmed.lastIndexOf('@') || at == trimmed.length() - 1) {
            throw new IllegalArgumentException("invalid email address");
        }
        return trimmed.substring(0, 1) + "***" + trimmed.substring(at);
    }

    public static final class LocalMailMessage {
        private final String type;
        private final String maskedRecipient;
        private final String contents;
        private final Instant createdAt;

        private LocalMailMessage(String type, String maskedRecipient, String contents, Instant createdAt) {
            this.type = type;
            this.maskedRecipient = maskedRecipient;
            this.contents = contents;
            this.createdAt = createdAt;
        }

        public String getType() {
            return type;
        }

        public String getMaskedRecipient() {
            return maskedRecipient;
        }

        public String getContents() {
            return contents;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }
    }
}
