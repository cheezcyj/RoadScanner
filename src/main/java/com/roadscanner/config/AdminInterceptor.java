package com.roadscanner.config;

import com.roadscanner.domain.user.MemberVO;
import com.roadscanner.service.user.UserService;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class AdminInterceptor implements HandlerInterceptor {
    private static final int ADMIN_GRADE = 2;

    private final UserService userService;

    public AdminInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        // session 객체 생성
        HttpSession session = request.getSession(false);
        MemberVO user = session == null ? null : (MemberVO) session.getAttribute("user");

        if (user == null || user.getId() == null) {
            return reject(response);
        }

        MemberVO lookup = new MemberVO();
        lookup.setId(user.getId());
        MemberVO currentUser = userService.selectUser(lookup);
        if (currentUser == null
                || currentUser.getCredentialVersion() != user.getCredentialVersion()) {
            session.invalidate();
            return reject(response);
        }

        currentUser.setPassword(null);
        session.setAttribute("user", currentUser);
        if (currentUser.getGrade() != ADMIN_GRADE) {
            return reject(response);
        }

        return true;  // 다음 핸들러로 이동
    }

    private boolean reject(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"관리자만 접근할 수 있습니다.\"}");
        return false;
    }
}
