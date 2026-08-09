package com.roadscanner.config;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import com.roadscanner.domain.user.MemberVO;

/**
 * Writes one privacy-conscious access log entry for each application request.
 * Request bodies, query strings, headers, cookies, and session identifiers are
 * deliberately excluded.
 */
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOG = LogManager.getLogger(RequestLoggingFilter.class);
    private static final String USER_SESSION_ATTRIBUTE = "user";
    private static final String ANONYMOUS_ACTOR = "anonymous";
    private static final String MEMBER_ACTOR = "member";
    private static final String ADMIN_ACTOR = "admin";
    private static final int ADMIN_GRADE = 2;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
            return false;
        }

        String path = requestPath(request);
        return "/favicon.ico".equals(path)
                || path.startsWith("/resources/")
                || path.startsWith("/webjars/")
                || path.startsWith("/local-files/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        String actorBeforeRequest = currentActor(request);
        boolean completed = false;

        try {
            filterChain.doFilter(request, response);
            completed = true;
        } finally {
            int status = response.getStatus();
            boolean failed = !completed;
            if (failed && !response.isCommitted()) {
                status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            }

            String actorAfterRequest = currentActor(request);
            String actor = actorAfterRequest != null
                    ? actorAfterRequest
                    : actorBeforeRequest != null ? actorBeforeRequest : ANONYMOUS_ACTOR;
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

            writeLog(
                    safeLogValue(request.getMethod(), 16, "UNKNOWN"),
                    mappedPath(request),
                    status,
                    durationMs,
                    actor,
                    failed ? "exception" : "completed");
        }
    }

    protected void writeLog(String method, String path, int status, long durationMs,
            String actor, String outcome) {
        if ("exception".equals(outcome) || status >= HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
            LOG.error("HTTP method={} path={} status={} durationMs={} actor={} outcome={}",
                    method, path, status, durationMs, actor, outcome);
        } else if (status >= HttpServletResponse.SC_BAD_REQUEST) {
            LOG.warn("HTTP method={} path={} status={} durationMs={} actor={} outcome={}",
                    method, path, status, durationMs, actor, outcome);
        } else {
            LOG.info("HTTP method={} path={} status={} durationMs={} actor={} outcome={}",
                    method, path, status, durationMs, actor, outcome);
        }
    }

    private static String mappedPath(HttpServletRequest request) {
        Object matchingPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (matchingPattern != null) {
            String pattern = matchingPattern.toString();
            if (!pattern.isEmpty()) {
                return safeLogValue(withoutPathParameters(pattern), 512, "/");
            }
        }
        return requestPath(request);
    }

    static String requestPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        if (requestUri == null || requestUri.isEmpty()) {
            return "/";
        }

        String contextPath = request.getContextPath();
        String path = contextPath != null && !contextPath.isEmpty()
                && requestUri.startsWith(contextPath)
                        ? requestUri.substring(contextPath.length())
                        : requestUri;
        if (path.isEmpty()) {
            path = "/";
        }

        return safeLogValue(withoutPathParameters(path), 512, "/");
    }

    private String currentActor(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                return null;
            }

            Object sessionUser = session.getAttribute(USER_SESSION_ATTRIBUTE);
            if (!(sessionUser instanceof MemberVO)) {
                return null;
            }

            MemberVO member = (MemberVO) sessionUser;
            String userId = member.getId();
            if (userId == null || userId.trim().isEmpty()) {
                return null;
            }
            return member.getGrade() == ADMIN_GRADE ? ADMIN_ACTOR : MEMBER_ACTOR;
        } catch (IllegalStateException ignored) {
            // A logout handler may invalidate the session before this filter completes.
            return null;
        }
    }

    private static String withoutPathParameters(String path) {
        StringBuilder sanitizedPath = new StringBuilder(path.length());
        boolean insideParameter = false;
        for (int index = 0; index < path.length(); index++) {
            char character = path.charAt(index);
            if (character == ';') {
                insideParameter = true;
            } else if (character == '/') {
                insideParameter = false;
                sanitizedPath.append(character);
            } else if (!insideParameter) {
                sanitizedPath.append(character);
            }
        }
        return sanitizedPath.toString();
    }

    private static String safeLogValue(String value, int maximumLength, String fallback) {
        if (value == null || value.isEmpty()) {
            return fallback;
        }

        int length = Math.min(value.length(), maximumLength);
        StringBuilder sanitized = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            char character = value.charAt(index);
            sanitized.append(Character.isISOControl(character) ? '_' : character);
        }
        return sanitized.toString();
    }
}
