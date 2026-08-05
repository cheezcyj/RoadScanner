package com.roadscanner.config;

import com.roadscanner.domain.user.MemberVO;
import com.roadscanner.service.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Slf4j
public class LoginInterceptor implements HandlerInterceptor {
    private static final int MEMBER_GRADE = 1;
    private static final int ADMIN_GRADE = 2;

    private final UserService userService;

    public LoginInterceptor(UserService userService) {
        this.userService = userService;
    }

    /**
     * Controller로 보내기 전에 호출
     * false 발생하면 controller를 호출하지 않음
     * Object는 핸들러 정보를 의미(RequestMapping, DefaultServletHandler)
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        // session 객체 생성
        HttpSession session = request.getSession(false);

        MemberVO sessionUser = session == null ? null : (MemberVO) session.getAttribute("user");
        if (sessionUser == null || sessionUser.getId() == null) {
            return rejectUnauthenticated(request, response);
        }

        MemberVO lookup = new MemberVO();
        lookup.setId(sessionUser.getId());
        MemberVO currentUser = userService.selectUser(lookup);
        if (currentUser == null
                || !isActiveGrade(currentUser.getGrade())
                || currentUser.getCredentialVersion() != sessionUser.getCredentialVersion()) {
            session.invalidate();
            return rejectUnauthenticated(request, response);
        }

        // Refresh role changes without ever retaining the stored password hash in the session.
        currentUser.setPassword(null);
        session.setAttribute("user", currentUser);

        return true;
    }

    private boolean isActiveGrade(int grade) {
        return grade == MEMBER_GRADE || grade == ADMIN_GRADE;
    }

    private boolean rejectUnauthenticated(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String requestPath = requestUri.startsWith(contextPath)
                ? requestUri.substring(contextPath.length())
                : requestUri;
        if (requestPath.startsWith("/api/")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"message\":\"로그인이 필요합니다.\"}");
            return false;
        }
        response.sendRedirect(request.getContextPath() + "/login");
        return false;
    }


    /**
     * view까지 처리가 끝나면 호출
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {

        // 예외 발생에 대한 로그 남기기
        if (ex != null) {
            log.error("Exception occurred in LoginInterceptor:", ex);
        }
    }

} // class end
