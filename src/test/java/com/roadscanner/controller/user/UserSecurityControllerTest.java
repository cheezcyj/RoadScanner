package com.roadscanner.controller.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpSession;

import com.roadscanner.domain.user.MemberVO;
import com.roadscanner.service.user.UserService;
import com.roadscanner.service.user.EmailVerificationService;
import com.roadscanner.service.user.LoginAttemptGuard;
import com.roadscanner.service.user.MailSendService;

@RunWith(MockitoJUnitRunner.class)
public class UserSecurityControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private MailSendService mailSendService;

    private UserInfoController userInfoController;
    private WithdrawController withdrawController;

    @Before
    public void setUp() {
        userInfoController = new UserInfoController();
        userInfoController.userService = userService;
        withdrawController = new WithdrawController();
        withdrawController.userService = userService;
    }

    @Test
    public void profileUpdateUsesAuthenticatedIdentity() throws Exception {
        MemberVO request = member("victim", "spoofed@example.com", "NewPassword1!");
        request.setCurrentPassword("CurrentPassword1!");
        MemberVO authenticated = member("owner", "owner@example.com", null);
        when(userService.doChangeInfo(request)).thenReturn(1);
        HttpSession session = mock(HttpSession.class);

        String response = userInfoController.doChangeInfo(request, authenticated, session);

        assertThat(response).contains("\"msgId\":\"10\"");
        assertThat(request.getId()).isEqualTo("owner");
        assertThat(request.getEmail()).isEqualTo("owner@example.com");
        verify(userService).doChangeInfo(request);
        verify(session).invalidate();
    }

    @Test
    public void withdrawalUsesAuthenticatedIdentityAndSubmittedPassword() throws Exception {
        MemberVO request = member("victim", null, "CurrentPassword1!");
        MemberVO authenticated = member("owner", "owner@example.com", null);
        when(userService.doWithdraw(request)).thenReturn(1);
        HttpSession session = mock(HttpSession.class);

        String response = withdrawController.withdraw(request, authenticated, session);

        assertThat(response).contains("\"msgId\":\"10\"");
        assertThat(request.getId()).isEqualTo("owner");
        assertThat(request.getPassword()).isEqualTo("CurrentPassword1!");
        verify(userService).doWithdraw(request);
        verify(session).invalidate();
    }

    @Test
    public void failedWithdrawalKeepsAuthenticatedSession() throws Exception {
        MemberVO request = member("victim", null, "WrongPassword1!");
        MemberVO authenticated = member("owner", "owner@example.com", null);
        HttpSession session = mock(HttpSession.class);
        when(userService.doWithdraw(request)).thenReturn(0);

        String response = withdrawController.withdraw(request, authenticated, session);

        assertThat(response).contains("\"msgId\":\"20\"");
        assertThat(request.getId()).isEqualTo("owner");
        verify(session, never()).invalidate();
    }

    @Test
    public void administratorCannotDeleteOwnAccountThroughManagementEndpoint() throws Exception {
        MemberVO request = member("administrator", null, null);
        MemberVO authenticated = member("administrator", "admin@example.com", null);

        String response = withdrawController.delete(request, authenticated);

        assertThat(response).contains("\"msgId\":\"20\"");
        verify(userService, never()).delete(any(MemberVO.class));
    }

    @Test
    public void administratorCannotChangeOwnGradeThroughManagementEndpoints() throws Exception {
        MemberVO request = member("administrator", null, null);
        MemberVO authenticated = member("administrator", "admin@example.com", null);

        String forbiddenResponse = withdrawController.forbidden(request, authenticated);
        String clearResponse = withdrawController.clear(request, authenticated);

        assertThat(forbiddenResponse).contains("\"msgId\":\"20\"");
        assertThat(clearResponse).contains("\"msgId\":\"20\"");
        verify(userService, never()).forbiddenGrade(any(MemberVO.class));
        verify(userService, never()).clearGrade(any(MemberVO.class));
    }

    @Test
    public void successfulLoginRotatesSessionAndDropsPasswordHash() throws Exception {
        LoginController controller = new LoginController();
        controller.userService = userService;
		LoginAttemptGuard loginAttemptGuard = mock(LoginAttemptGuard.class);
		controller.loginAttemptGuard = loginAttemptGuard;
        MemberVO credentials = member("owner", null, "CurrentPassword1!");
        MemberVO databaseUser = member("owner", "owner@example.com", "$2a$10$storedHash");
        when(userService.doLogin(credentials)).thenReturn(30);
        when(userService.selectUser(credentials)).thenReturn(databaseUser);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession oldSession = mock(HttpSession.class);
        HttpSession newSession = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(oldSession);
        when(request.getSession(true)).thenReturn(newSession);
		when(request.getRemoteAddr()).thenReturn("192.0.2.10");

        String response = controller.loginButtonEvent(credentials, request);

        assertThat(response).contains("\"msgId\":\"30\"");
        assertThat(databaseUser.getPassword()).isNull();
        verify(oldSession).invalidate();
        verify(newSession).setAttribute(eq("user"), eq(databaseUser));
		verify(loginAttemptGuard).recordSuccess("owner");
    }

	@Test
	public void loginRejectsAccountChangedAfterPasswordSnapshot() throws Exception {
		LoginController controller = new LoginController();
		controller.userService = userService;
		LoginAttemptGuard loginAttemptGuard = mock(LoginAttemptGuard.class);
		controller.loginAttemptGuard = loginAttemptGuard;
		MemberVO credentials = member("owner", null, "CurrentPassword1!");
		credentials.setCredentialVersion(3);
		MemberVO databaseUser = member("owner", "owner@example.com", "$2a$10$changedHash");
		databaseUser.setCredentialVersion(4);
		when(userService.doLogin(credentials)).thenReturn(30);
		when(userService.selectUser(credentials)).thenReturn(databaseUser);
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("192.0.2.10");

		String response = controller.loginButtonEvent(credentials, request);

		assertThat(response).contains("\"msgId\":\"20\"");
		verify(request, never()).getSession(true);
		verify(loginAttemptGuard).recordFailure("owner", "192.0.2.10");
	}

    @Test
    public void loginFailuresDoNotRevealIdentifierOrAccountState() throws Exception {
        LoginController controller = new LoginController();
        controller.userService = userService;
        MemberVO credentials = member("private-member", null, "WrongPassword1!");
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(userService.doLogin(credentials)).thenReturn(10, 20, 40);

        String missingAccount = controller.loginButtonEvent(credentials, request);
        String wrongPassword = controller.loginButtonEvent(credentials, request);
        String suspendedAccount = controller.loginButtonEvent(credentials, request);

        assertThat(missingAccount).isEqualTo(wrongPassword).isEqualTo(suspendedAccount);
        assertThat(missingAccount)
                .contains("\"msgId\":\"20\"")
                .doesNotContain("private-member")
                .doesNotContain("정지");
    }

    @Test
    public void repeatedFailuresTemporarilyStopAuthenticationByAccountAndAddress() throws Exception {
        LoginController controller = new LoginController();
        controller.userService = userService;
        MemberVO credentials = member("private-member", null, "WrongPassword1!");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("192.0.2.20");
        when(userService.doLogin(credentials)).thenReturn(20);

        String response = null;
        for (int attempt = 0; attempt < 6; attempt++) {
            response = controller.loginButtonEvent(credentials, request);
        }

        verify(userService, times(5)).doLogin(credentials);
        assertThat(response)
                .contains("\"msgId\":\"20\"")
                .doesNotContain("private-member")
                .doesNotContain("192.0.2.20");
    }

    @Test
    public void idRecoveryNeverReturnsIdentifierAndNewSessionDoesNotResetMailLimit() throws Exception {
        LoginController controller = new LoginController();
        controller.userService = userService;
        controller.emailVerificationService = new EmailVerificationService();
        controller.mailSend = mailSendService;
        MemberVO request = member(null, "Owner@Example.com", null);
        when(userService.doSearchId(request)).thenReturn("private-user-id");

        String firstResponse = controller.findId(request, new MockHttpSession());
        String secondResponse = controller.findId(request, new MockHttpSession());

        assertThat(firstResponse)
                .contains("\"msgId\":\"30\"")
                .doesNotContain("private-user-id");
        assertThat(secondResponse).doesNotContain("private-user-id");
        verify(mailSendService, times(1)).findId("Owner@example.com", "private-user-id");
    }

    @Test
    public void passwordRecoveryEntryDoesNotRevealAccountExistence() throws Exception {
        LoginController controller = new LoginController();
        controller.userService = userService;
        MemberVO request = member("unknown", "unknown@example.com", null);

        String response = controller.findPw(request, new MockHttpSession());

        assertThat(response)
                .contains("\"msgId\":\"30\"")
                .doesNotContain("unknown@example.com")
                .doesNotContain("정지");
        verify(userService, never()).doSearchPw(any(MemberVO.class));
    }

	@Test
	public void publicRegistrationEmailCheckDoesNotQueryAccountExistence() throws Exception {
		LoginController controller = new LoginController();
		controller.userService = userService;
		MemberVO request = member(null, "private@example.com", null);

		String response = controller.emailDulpCheck(request, new MockHttpSession());

		assertThat(response).contains("\"msgId\":\"20\"");
		verify(userService, never()).doEmailDuplCheck(any(MemberVO.class));
	}

    @Test
    public void profileUpdateRejectsWeakPasswordBeforeServiceCall() throws Exception {
        MemberVO request = member("victim", "spoofed@example.com", "weak");

        String response = userInfoController.doChangeInfo(
                request,
                member("owner", "owner@example.com", null),
                mock(HttpSession.class));

        assertThat(response).contains("\"msgId\":\"20\"");
        verify(userService, never()).doChangeInfo(any(MemberVO.class));
    }

    @Test
    public void registrationRejectsInvalidCredentialsBeforeConsumingProof() throws Exception {
        LoginController controller = new LoginController();
        controller.userService = userService;
        controller.emailVerificationService = emailVerificationService;
        MemberVO request = member("bad", "owner@example.com", "weak");

        String response = controller.membershipRegister(
                request, "verification-proof", new MockHttpSession());

        assertThat(response).contains("\"msgId\":\"20\"");
        verify(emailVerificationService, never()).consume(
                any(HttpSession.class), anyString(), any(EmailVerificationService.Purpose.class), anyString());
        verify(userService, never()).register(any(MemberVO.class));
    }

    private MemberVO member(String id, String email, String password) {
        MemberVO member = new MemberVO();
        member.setId(id);
        member.setEmail(email);
        member.setPassword(password);
        member.setGrade(1);
        return member;
    }
}
