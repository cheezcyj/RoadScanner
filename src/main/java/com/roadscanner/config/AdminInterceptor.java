package com.roadscanner.config;

import com.roadscanner.domain.user.MemberVO;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        // session 객체 생성
        HttpSession session = request.getSession();
        MemberVO user = (MemberVO) session.getAttribute("user");

        if (user == null || user.getGrade() != 2) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"message\":\"관리자만 접근할 수 있습니다.\"}");

            return false;
        }

        return true;  // 다음 핸들러로 이동
    }
}
