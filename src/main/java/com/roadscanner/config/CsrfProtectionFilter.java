package com.roadscanner.config;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Synchronizer-token CSRF protection for browser requests.
 */
public class CsrfProtectionFilter extends OncePerRequestFilter {

    static final String SESSION_ATTRIBUTE = CsrfProtectionFilter.class.getName() + ".TOKEN";
    static final String REQUEST_ATTRIBUTE = "csrfToken";
    static final String HEADER_NAME = "X-CSRF-Token";
    static final String PARAMETER_NAME = "_csrf";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String resourcePrefix = (contextPath == null ? "" : contextPath) + "/resources/";
        return request.getRequestURI().startsWith(resourcePrefix);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String token = getOrCreateToken(request);
        request.setAttribute(REQUEST_ATTRIBUTE, token);
        request.setAttribute("csrfHeaderName", HEADER_NAME);
        request.setAttribute("csrfParameterName", PARAMETER_NAME);

        if (isStateChanging(request) && !hasValidToken(request, token)) {
            reject(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getOrCreateToken(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        Object existing = session.getAttribute(SESSION_ATTRIBUTE);
        if (existing instanceof String && !((String) existing).isEmpty()) {
            return (String) existing;
        }

        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        session.setAttribute(SESSION_ATTRIBUTE, token);
        return token;
    }

    private boolean isStateChanging(HttpServletRequest request) {
        String method = request.getMethod().toUpperCase(Locale.ROOT);
        return "POST".equals(method) || "PUT".equals(method)
                || "PATCH".equals(method) || "DELETE".equals(method);
    }

    private boolean hasValidToken(HttpServletRequest request, String expectedToken) {
        String submittedToken = request.getHeader(HEADER_NAME);
        if (submittedToken == null || submittedToken.isEmpty()) {
            submittedToken = request.getParameter(PARAMETER_NAME);
        }
        if (submittedToken == null) {
            return false;
        }

        return MessageDigest.isEqual(
                expectedToken.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                submittedToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"CSRF token validation failed\"}");
    }
}
