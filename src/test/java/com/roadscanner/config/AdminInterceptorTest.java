package com.roadscanner.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import com.roadscanner.domain.user.MemberVO;
import com.roadscanner.service.user.UserService;

public class AdminInterceptorTest {

    private AdminInterceptor interceptor;
    private UserService userService;

    @Before
    public void setUp() {
        userService = mock(UserService.class);
        interceptor = new AdminInterceptor(userService);
    }

    @Test
    public void unauthenticatedRequestIsForbiddenWithoutCreatingSession() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertForbiddenResponse(response);
        assertNull(request.getSession(false));
    }

    @Test
    public void normalUserRequestIsForbidden() throws Exception {
        MockHttpServletRequest request = requestWithGrade(1);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertForbiddenResponse(response);
    }

    @Test
    public void administratorRequestContinues() throws Exception {
        MockHttpServletRequest request = requestWithGrade(2);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertTrue(allowed);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    public void demotedAdministratorLosesAccessImmediately() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = (MockHttpSession) request.getSession();
        session.setAttribute("user", new MemberVO("member", null, "member@example.com", 2));
        MemberVO currentUser = new MemberVO("member", "stored-hash", "member@example.com", 1);
        when(userService.selectUser(org.mockito.ArgumentMatchers.any(MemberVO.class)))
                .thenReturn(currentUser);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertForbiddenResponse(response);
        assertEquals(currentUser, session.getAttribute("user"));
        assertNull(currentUser.getPassword());
    }

    @Test
    public void deletedAdministratorSessionIsInvalidated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = (MockHttpSession) request.getSession();
        session.setAttribute("user", new MemberVO("deleted", null, "member@example.com", 2));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertTrue(session.isInvalid());
        assertForbiddenResponse(response);
    }

	@Test
	public void passwordChangeInvalidatesOlderAdministratorSession() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpSession session = (MockHttpSession) request.getSession();
		MemberVO sessionUser = new MemberVO("admin", null, "admin@example.com", 2);
		sessionUser.setCredentialVersion(7);
		session.setAttribute("user", sessionUser);
		MemberVO currentUser = new MemberVO("admin", "stored-hash", "admin@example.com", 2);
		currentUser.setCredentialVersion(8);
		when(userService.selectUser(org.mockito.ArgumentMatchers.any(MemberVO.class)))
				.thenReturn(currentUser);
		MockHttpServletResponse response = new MockHttpServletResponse();

		boolean allowed = interceptor.preHandle(request, response, new Object());

		assertFalse(allowed);
		assertTrue(session.isInvalid());
		assertForbiddenResponse(response);
	}

    private MockHttpServletRequest requestWithGrade(int grade) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MemberVO sessionUser = new MemberVO("member", null, "member@example.com", grade);
        request.getSession().setAttribute("user", sessionUser);
        whenSelectReturnsGrade(grade);
        return request;
    }

    private void whenSelectReturnsGrade(int grade) {
        MemberVO currentUser = new MemberVO("member", "stored-hash", "member@example.com", grade);
        try {
            when(userService.selectUser(org.mockito.ArgumentMatchers.any(MemberVO.class)))
                    .thenReturn(currentUser);
        } catch (java.sql.SQLException exception) {
            throw new AssertionError(exception);
        }
    }

    private void assertForbiddenResponse(MockHttpServletResponse response) throws Exception {
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertEquals("UTF-8", response.getCharacterEncoding());
        assertEquals("{\"message\":\"관리자만 접근할 수 있습니다.\"}", response.getContentAsString());
    }
}
