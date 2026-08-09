package com.roadscanner.controller.user;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.mail.MessagingException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import com.roadscanner.domain.user.MemberVO;
import com.roadscanner.service.user.EmailVerificationService;
import com.roadscanner.service.user.MailSendService;
import com.roadscanner.service.user.UserService;

public class EmailVerificationControllerTest {

    private UserService userService;
    private MailSendService mailSendService;
    private LoginController loginController;
    private UserInfoController userInfoController;

    @Before
    public void setUp() throws Exception {
        userService = mock(UserService.class);
        mailSendService = mock(MailSendService.class);
        when(userService.doEmailDuplCheck(any(MemberVO.class))).thenReturn(10);
        EmailVerificationService verificationService = new EmailVerificationService();

        loginController = new LoginController();
        loginController.userService = userService;
        loginController.mailSend = mailSendService;
        loginController.emailVerificationService = verificationService;

        userInfoController = new UserInfoController();
        userInfoController.userService = userService;
        userInfoController.mailSend = mailSendService;
        userInfoController.emailVerificationService = verificationService;
    }

    @Test
    public void registrationResponseNeverReturnsCodeAndGrantIsOneTime() throws Exception {
        MockHttpSession session = new MockHttpSession();
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);

        String sendResponse = loginController.mailCheck("member@example.com", session);
        verify(mailSendService).sendRegistrationVerification(eq("member@example.com"), code.capture());

        assertTrue(code.getValue().matches("\\d{6}"));
        assertFalse(sendResponse.contains(code.getValue()));
        assertTrue(sendResponse.contains("\"msgId\":\"10\""));
        MockHttpServletResponse verificationResponse = new MockHttpServletResponse();
        String verifyResponse = loginController.verifyRegistrationEmail(
                "member@example.com",
                code.getValue(),
                session,
                verificationResponse);
        assertTrue(verifyResponse.contains("\"msgId\":\"10\""));
        String verificationToken = verificationResponse.getHeader("X-Email-Verification-Token");
        assertTrue(verificationToken != null && !verificationToken.isEmpty());

        MemberVO wrongEmail = member("newmember1", "other@example.com");
        assertTrue(loginController.membershipRegister(
                wrongEmail,
                verificationToken,
                session).contains("\"msgId\":\"20\""));
        verify(userService, never()).register(wrongEmail);

        MemberVO verifiedEmail = member("newmember1", "member@example.com");
        assertTrue(loginController.membershipRegister(
                verifiedEmail,
                null,
                session).contains("\"msgId\":\"20\""));
        verify(userService, never()).register(verifiedEmail);
        when(userService.register(verifiedEmail)).thenReturn(10);
        assertTrue(loginController.membershipRegister(
                verifiedEmail,
                verificationToken,
                session).contains("\"msgId\":\"10\""));
        assertTrue(loginController.membershipRegister(
                verifiedEmail,
                verificationToken,
                session).contains("\"msgId\":\"20\""));
        verify(userService).register(verifiedEmail);
    }

    @Test
    public void passwordResetResponseNeverReturnsCodeAndRejectsAnotherEmail() throws Exception {
        MockHttpSession session = new MockHttpSession();
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);

        String sendResponse = userInfoController.change_mailCheck("member@example.com", session);
        verify(mailSendService).sendPasswordResetVerification(eq("member@example.com"), code.capture());

        assertFalse(sendResponse.contains(code.getValue()));
        MockHttpServletResponse verificationResponse = new MockHttpServletResponse();
        String verifyResponse = userInfoController.verifyPasswordResetEmail(
                "member@example.com",
                code.getValue(),
                session,
                verificationResponse);
        assertTrue(verifyResponse.contains("\"msgId\":\"10\""));
        String verificationToken = verificationResponse.getHeader("X-Email-Verification-Token");
        assertTrue(verificationToken != null && !verificationToken.isEmpty());

        MemberVO wrongEmail = member(null, "other@example.com");
        assertTrue(userInfoController.changePw(
                wrongEmail,
                verificationToken,
                session).contains("\"msgId\":\"20\""));
        verify(userService, never()).changePw(wrongEmail);

        MemberVO verifiedEmail = member(null, "member@example.com");
        assertTrue(userInfoController.changePw(
                verifiedEmail,
                null,
                session).contains("\"msgId\":\"20\""));
        verify(userService, never()).changePw(verifiedEmail);
        when(userService.changePw(verifiedEmail)).thenReturn(1);
        assertTrue(userInfoController.changePw(
                verifiedEmail,
                verificationToken,
                session).contains("\"msgId\":\"10\""));
        assertTrue(userInfoController.changePw(
                verifiedEmail,
                verificationToken,
                new MockHttpSession()).contains("\"msgId\":\"20\""));
        verify(userService).changePw(verifiedEmail);
    }

    @Test
    public void registrationAndPasswordResetGrantsCannotBeInterchanged() throws Exception {
        MockHttpSession session = new MockHttpSession();
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        loginController.mailCheck("member@example.com", session);
        verify(mailSendService).sendRegistrationVerification(eq("member@example.com"), code.capture());
        MockHttpServletResponse verificationResponse = new MockHttpServletResponse();
        loginController.verifyRegistrationEmail(
                "member@example.com",
                code.getValue(),
                session,
                verificationResponse);

        MemberVO reset = member(null, "member@example.com");
        assertTrue(userInfoController.changePw(
                reset,
                verificationResponse.getHeader("X-Email-Verification-Token"),
                session).contains("\"msgId\":\"20\""));
        verify(userService, never()).changePw(any(MemberVO.class));
    }

    @Test
    public void failedMailDeliveryClearsPendingChallenge() throws Exception {
        MockHttpSession session = new MockHttpSession();
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        doThrow(new MessagingException("mail unavailable"))
                .when(mailSendService)
                .sendRegistrationVerification(eq("member@example.com"), any(String.class));

        try {
            loginController.mailCheck("member@example.com", session);
            fail("Expected mail delivery failure");
        } catch (MessagingException expected) {
            verify(mailSendService).sendRegistrationVerification(eq("member@example.com"), code.capture());
        }

        assertTrue(loginController.verifyRegistrationEmail(
                "member@example.com",
                code.getValue(),
                session,
                new MockHttpServletResponse()).contains("\"msgId\":\"20\""));
    }

    @Test
    public void passwordResetSendDoesNotRevealMissingEmail() throws Exception {
        MockHttpSession session = new MockHttpSession();
        when(userService.doEmailDuplCheck(any(MemberVO.class))).thenReturn(20);

        String response = userInfoController.change_mailCheck("missing@example.com", session);
        String repeatedResponse = userInfoController.change_mailCheck("missing@example.com", session);

        assertTrue(response.contains("\"msgId\":\"10\""));
        assertFalse(response.contains("없습니다"));
        assertTrue(repeatedResponse.contains("\"msgId\":\"20\""));
        verify(mailSendService, never()).sendPasswordResetVerification(any(String.class), any(String.class));
    }

    @Test
    public void passwordResetMailFailureUsesGenericResponseAndClearsChallenge() throws Exception {
        MockHttpSession session = new MockHttpSession();
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        doThrow(new MessagingException("mail unavailable"))
                .when(mailSendService)
                .sendPasswordResetVerification(eq("member@example.com"), any(String.class));

        String response = userInfoController.change_mailCheck("member@example.com", session);
        verify(mailSendService).sendPasswordResetVerification(
                eq("member@example.com"),
                code.capture());

        assertTrue(response.contains("\"msgId\":\"10\""));
        assertFalse(response.contains(code.getValue()));
        assertTrue(userInfoController.verifyPasswordResetEmail(
                "member@example.com",
                code.getValue(),
                session,
                new MockHttpServletResponse()).contains("\"msgId\":\"20\""));
    }

    @Test
    public void repeatedSendIsRateLimitedBeforeCallingMailProvider() throws Exception {
        MockHttpSession session = new MockHttpSession();

        assertTrue(loginController.mailCheck(
                "member@example.com",
                session).contains("\"msgId\":\"10\""));
        assertTrue(loginController.mailCheck(
                "member@example.com",
                session).contains("\"msgId\":\"20\""));

        verify(mailSendService, times(1)).sendRegistrationVerification(
                eq("member@example.com"),
                any(String.class));
    }

    private MemberVO member(String id, String email) {
        return new MemberVO(id, "Password1!", email, 1);
    }

}
