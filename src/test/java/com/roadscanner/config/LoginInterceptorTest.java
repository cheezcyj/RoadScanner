package com.roadscanner.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import javax.servlet.http.HttpServletResponse;

import com.roadscanner.domain.user.MemberVO;
import com.roadscanner.service.user.UserService;

public class LoginInterceptorTest {

    private LoginInterceptor interceptor;
    private UserService userService;

    @Before
    public void setUp() {
        userService = mock(UserService.class);
        interceptor = new LoginInterceptor(userService);
    }

    @Test
    public void unauthenticatedRequestRedirectsWithoutCreatingSession() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/roadscanner");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals("/roadscanner/login", response.getRedirectedUrl());
        assertNull(request.getSession(false));
    }

    @Test
    public void authenticatedRequestContinuesWithExistingSession() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = (MockHttpSession) request.getSession();
        MemberVO sessionUser = new MemberVO("member", null, "member@example.com", 1);
        MemberVO currentUser = new MemberVO("member", "stored-hash", "member@example.com", 1);
        session.setAttribute("user", sessionUser);
        when(userService.selectUser(org.mockito.ArgumentMatchers.any(MemberVO.class)))
                .thenReturn(currentUser);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertTrue(allowed);
        assertSame(session, request.getSession(false));
        assertSame(currentUser, session.getAttribute("user"));
        assertNull(currentUser.getPassword());
    }

    @Test
    public void deletedAccountInvalidatesStaleSession() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/roadscanner/mypage");
        request.setContextPath("/roadscanner");
        MockHttpSession session = (MockHttpSession) request.getSession();
        session.setAttribute("user", new MemberVO("deleted", null, "member@example.com", 1));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertTrue(session.isInvalid());
        assertEquals("/roadscanner/login", response.getRedirectedUrl());
    }

    @Test
    public void bannedAccountInvalidatesStaleApiSession() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/roadscanner/api/qna/save");
        request.setContextPath("/roadscanner");
        MockHttpSession session = (MockHttpSession) request.getSession();
        session.setAttribute("user", new MemberVO("banned", null, "member@example.com", 1));
        MemberVO banned = new MemberVO("banned", "stored-hash", "member@example.com", 3);
        when(userService.selectUser(org.mockito.ArgumentMatchers.any(MemberVO.class))).thenReturn(banned);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertTrue(session.isInvalid());
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertEquals("{\"message\":\"로그인이 필요합니다.\"}", response.getContentAsString());
    }

	@Test
	public void passwordChangeInvalidatesOlderSessionVersion() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mypage");
		MockHttpSession session = (MockHttpSession) request.getSession();
		MemberVO sessionUser = new MemberVO("member", null, "member@example.com", 1);
		sessionUser.setCredentialVersion(3);
		session.setAttribute("user", sessionUser);
		MemberVO currentUser = new MemberVO("member", "stored-hash", "member@example.com", 1);
		currentUser.setCredentialVersion(4);
		when(userService.selectUser(org.mockito.ArgumentMatchers.any(MemberVO.class)))
				.thenReturn(currentUser);

		boolean allowed = interceptor.preHandle(
				request, new MockHttpServletResponse(), new Object());

		assertFalse(allowed);
		assertTrue(session.isInvalid());
	}

    @Test
    public void unauthenticatedApiRequestReturnsJsonUnauthorized() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/roadscanner/api/qna/7");
        request.setContextPath("/roadscanner");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertEquals("{\"message\":\"로그인이 필요합니다.\"}", response.getContentAsString());
        assertNull(request.getSession(false));
    }
}
