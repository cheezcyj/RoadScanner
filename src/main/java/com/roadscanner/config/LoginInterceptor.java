package com.roadscanner.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Slf4j
public class LoginInterceptor implements HandlerInterceptor {
    /**
     * Controller로 보내기 전에 호출
     * false 발생하면 controller를 호출하지 않음
     * Object는 핸들러 정보를 의미(RequestMapping, DefaultServletHandler)
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        // session 객체 생성
        HttpSession session = request.getSession();

        if (session.getAttribute("user") == null) {
            response.sendRedirect("/login");

            return false;
        } // if

        return true;
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
