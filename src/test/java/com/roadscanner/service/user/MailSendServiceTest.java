package com.roadscanner.service.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mail.javamail.JavaMailSender;

public class MailSendServiceTest {

    private JavaMailSender mailSender;
    private MailSendService service;

    @Before
    public void setUp() {
        mailSender = mock(JavaMailSender.class);
        service = new MailSendService(mailSender, "no-reply@example.test");
    }

    @Test
    public void registrationMailContainsServerGeneratedCode() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);

        service.sendRegistrationVerification("member@example.com", "123456");

        verify(mailSender).send(message);
        assertEquals("회원 가입 인증 이메일 입니다.", message.getSubject());
        assertEquals("member@example.com", message.getRecipients(Message.RecipientType.TO)[0].toString());
        assertTrue(message.getContent().toString().contains("123456"));
    }

    @Test
    public void verificationMailRejectsMalformedCode() throws Exception {
        try {
            service.sendPasswordResetVerification("member@example.com", "12345");
            fail("Expected malformed code to be rejected");
        } catch (IllegalArgumentException expected) {
            verify(mailSender, never()).createMimeMessage();
        }
    }

    @Test
    public void verificationMailRejectsRecipientHeaderInjection() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);

        try {
            service.sendRegistrationVerification(
                    "member@example.com\r\nBcc: attacker@example.com",
                    "123456");
            fail("Expected recipient header injection to be rejected");
        } catch (IllegalArgumentException expected) {
            verify(mailSender, never()).send(any(MimeMessage.class));
        }
    }
}
