package com.roadscanner.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.FilterChain;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartHttpServletRequest;

public class CsrfProtectionFilterTest {

    private final CsrfProtectionFilter filter = new CsrfProtectionFilter();

    @Test
    public void safeRequestCreatesAndExposesToken() throws Exception {
        MockHttpServletRequest request = request("GET", "/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean called = new AtomicBoolean();

        filter.doFilter(request, response, recordingChain(called));

        assertTrue(called.get());
        assertNotNull(request.getAttribute(CsrfProtectionFilter.REQUEST_ATTRIBUTE));
        assertNotNull(request.getSession(false).getAttribute(CsrfProtectionFilter.SESSION_ATTRIBUTE));
    }

    @Test
    public void stateChangingRequestWithoutTokenIsRejected() throws Exception {
        MockHttpServletRequest request = request("POST", "/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean called = new AtomicBoolean();

        filter.doFilter(request, response, recordingChain(called));

        assertFalse(called.get());
        assertEquals(403, response.getStatus());
    }

    @Test
    public void matchingHeaderAllowsStateChangingRequest() throws Exception {
        MockHttpServletRequest request = request("POST", "/login");
        request.getSession(true).setAttribute(CsrfProtectionFilter.SESSION_ATTRIBUTE, "known-token");
        request.addHeader(CsrfProtectionFilter.HEADER_NAME, "known-token");
        AtomicBoolean called = new AtomicBoolean();

        filter.doFilter(request, new MockHttpServletResponse(), recordingChain(called));

        assertTrue(called.get());
    }

    @Test
    public void matchingFormParameterAllowsStateChangingRequest() throws Exception {
        MockHttpServletRequest request = request("DELETE", "/qna/fileDelete");
        request.getSession(true).setAttribute(CsrfProtectionFilter.SESSION_ATTRIBUTE, "known-token");
        request.addParameter(CsrfProtectionFilter.PARAMETER_NAME, "known-token");
        AtomicBoolean called = new AtomicBoolean();

        filter.doFilter(request, new MockHttpServletResponse(), recordingChain(called));

        assertTrue(called.get());
    }

    @Test
    public void parsedMultipartFormParameterAllowsStateChangingRequest() throws Exception {
        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/main/fileUploaded");
        request.getSession(true).setAttribute(CsrfProtectionFilter.SESSION_ATTRIBUTE, "known-token");
        request.addParameter(CsrfProtectionFilter.PARAMETER_NAME, "known-token");
        AtomicBoolean called = new AtomicBoolean();

        filter.doFilter(request, new MockHttpServletResponse(), recordingChain(called));

        assertTrue(called.get());
    }

    @Test
    public void wrongTokenIsRejected() throws Exception {
        MockHttpServletRequest request = request("PUT", "/api/qna/1");
        request.getSession(true).setAttribute(CsrfProtectionFilter.SESSION_ATTRIBUTE, "expected");
        request.addHeader(CsrfProtectionFilter.HEADER_NAME, "wrong");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, recordingChain(new AtomicBoolean()));

        assertEquals(403, response.getStatus());
    }

    @Test
    public void staticResourceDoesNotCreateSession() throws Exception {
        MockHttpServletRequest request = request("GET", "/resources/js/app.js");
        AtomicBoolean called = new AtomicBoolean();

        filter.doFilter(request, new MockHttpServletResponse(), recordingChain(called));

        assertTrue(called.get());
        assertNull(request.getSession(false));
    }

    private MockHttpServletRequest request(String method, String uri) {
        return new MockHttpServletRequest(method, uri);
    }

    private FilterChain recordingChain(final AtomicBoolean called) {
        return (request, response) -> called.set(true);
    }
}
