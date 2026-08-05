package com.roadscanner.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.roadscanner.domain.user.MemberVO;

public class RequestLoggingFilterTest {

    private final RecordingRequestLoggingFilter filter = new RecordingRequestLoggingFilter();

    @Test
    public void logsMethodPathStatusDurationAndAnonymousUser() throws Exception {
        MockHttpServletRequest request = request("GET", "/roadscanner/qna");
        request.setContextPath("/roadscanner");
        request.setQueryString("token=must-not-be-logged");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                ((HttpServletResponse) servletResponse).setStatus(201));

        LogEntry entry = filter.onlyEntry();
        assertEquals("GET", entry.method);
        assertEquals("/qna", entry.path);
        assertEquals(201, entry.status);
        assertTrue(entry.durationMs >= 0);
        assertEquals("anonymous", entry.actor);
        assertEquals("completed", entry.outcome);
        assertFalse(entry.path.contains("token"));
        assertNull(request.getSession(false));
    }

    @Test
    public void logsAuthenticatedMemberWithoutExposingUserId() throws Exception {
        MockHttpServletRequest request = request("POST", "/mypage");
        MemberVO user = new MemberVO();
        user.setId("private-member-id");
        user.setGrade(1);
        request.getSession(true).setAttribute("user", user);

        filter.doFilter(request, new MockHttpServletResponse(), (servletRequest, servletResponse) -> {
        });

        assertEquals("member", filter.onlyEntry().actor);
    }

    @Test
    public void usesNewUserAfterSuccessfulLogin() throws Exception {
        MockHttpServletRequest request = request("POST", "/login");
        request.getSession(true).setAttribute("preLoginState", "present");

        filter.doFilter(request, new MockHttpServletResponse(), (servletRequest, servletResponse) -> {
            HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
            httpRequest.getSession(false).invalidate();
            MemberVO user = new MemberVO();
            user.setId("localuser");
            user.setGrade(1);
            httpRequest.getSession(true).setAttribute("user", user);
        });

        assertEquals("member", filter.onlyEntry().actor);
    }

    @Test
    public void retainsUserForLogoutLogAfterSessionInvalidation() throws Exception {
        MockHttpServletRequest request = request("POST", "/logout");
        MemberVO user = new MemberVO();
        user.setId("localuser");
        user.setGrade(1);
        request.getSession(true).setAttribute("user", user);

        filter.doFilter(request, new MockHttpServletResponse(), (servletRequest, servletResponse) ->
                ((HttpServletRequest) servletRequest).getSession(false).invalidate());

        assertEquals("member", filter.onlyEntry().actor);
    }

    @Test
    public void recordsAdminRoleWithoutExposingUserId() throws Exception {
        MockHttpServletRequest request = request("GET", "/admin");
        MemberVO user = new MemberVO();
        user.setId("private-admin-id");
        user.setGrade(2);
        request.getSession(true).setAttribute("user", user);

        filter.doFilter(request, new MockHttpServletResponse(), (servletRequest, servletResponse) -> {
        });

        assertEquals("admin", filter.onlyEntry().actor);
    }

    @Test
    public void prefersControllerMappingPatternOverConcretePath() throws Exception {
        MockHttpServletRequest request = request("GET", "/api/qna/27");

        filter.doFilter(request, new MockHttpServletResponse(), (servletRequest, servletResponse) ->
                servletRequest.setAttribute(
                        org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
                        "/api/qna/{no}"));

        assertEquals("/api/qna/{no}", filter.onlyEntry().path);
    }

    @Test
    public void removesSessionIdentifiersAndOtherPathParameters() throws Exception {
        MockHttpServletRequest request = request(
                "GET", "/qna;tracking=private/17;jsessionid=must-not-be-logged");

        filter.doFilter(request, new MockHttpServletResponse(), (servletRequest, servletResponse) -> {
        });

        assertEquals("/qna/17", filter.onlyEntry().path);
    }

    @Test
    public void replacesControlCharactersInFallbackPath() throws Exception {
        MockHttpServletRequest request = request("GET", "/qna\r\nforged-entry");

        filter.doFilter(request, new MockHttpServletResponse(), (servletRequest, servletResponse) -> {
        });

        assertEquals("/qna__forged-entry", filter.onlyEntry().path);
    }

    @Test
    public void recordsRequestRejectedByCsrfFilter() throws Exception {
        MockHttpServletRequest request = request("POST", "/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean controllerCalled = new AtomicBoolean();
        CsrfProtectionFilter csrfFilter = new CsrfProtectionFilter();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                csrfFilter.doFilter(servletRequest, servletResponse,
                        (ignoredRequest, ignoredResponse) -> controllerCalled.set(true)));

        assertFalse(controllerCalled.get());
        assertEquals(403, response.getStatus());
        assertEquals(403, filter.onlyEntry().status);
    }

    @Test
    public void skipsOnlyReadRequestsForStaticAssets() throws Exception {
        String[] paths = { "/favicon.ico", "/resources/css/app.css", "/webjars/chart.js",
                "/local-files/image.png" };

        for (String path : paths) {
            AtomicBoolean called = new AtomicBoolean();
            filter.doFilter(request("GET", path), new MockHttpServletResponse(),
                    (servletRequest, servletResponse) -> called.set(true));
            assertTrue(called.get());
        }

        assertTrue(filter.entries.isEmpty());

        filter.doFilter(request("POST", "/resources/css/app.css"),
                new MockHttpServletResponse(), (servletRequest, servletResponse) -> {
                });

        assertEquals("/resources/css/app.css", filter.onlyEntry().path);
    }

    @Test
    public void recordsServerErrorWhenChainThrowsBeforeSettingStatus() throws Exception {
        MockHttpServletRequest request = request("GET", "/broken");
        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            filter.doFilter(request, response, (servletRequest, servletResponse) -> {
                ((HttpServletResponse) servletResponse).setStatus(404);
                throw new ServletException("boom");
            });
            fail("Expected ServletException");
        } catch (ServletException expected) {
            assertEquals("boom", expected.getMessage());
        }

        assertEquals(500, filter.onlyEntry().status);
        assertEquals("exception", filter.onlyEntry().outcome);
    }

    private MockHttpServletRequest request(String method, String uri) {
        return new MockHttpServletRequest(method, uri);
    }

    private static final class RecordingRequestLoggingFilter extends RequestLoggingFilter {
        private final List<LogEntry> entries = new ArrayList<>();

        @Override
        protected void writeLog(String method, String path, int status, long durationMs,
                String actor, String outcome) {
            entries.add(new LogEntry(method, path, status, durationMs, actor, outcome));
        }

        private LogEntry onlyEntry() {
            assertEquals(1, entries.size());
            return entries.get(0);
        }
    }

    private static final class LogEntry {
        private final String method;
        private final String path;
        private final int status;
        private final long durationMs;
        private final String actor;
        private final String outcome;

        private LogEntry(String method, String path, int status, long durationMs,
                String actor, String outcome) {
            this.method = method;
            this.path = path;
            this.status = status;
            this.durationMs = durationMs;
            this.actor = actor;
            this.outcome = outcome;
        }
    }
}
