package com.roadscanner.controller.user;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@Profile("local")
public class LocalMailboxModelAdvice {

    @ModelAttribute
    public void exposeLocalMailboxNavigation(HttpServletRequest request) {
        request.setAttribute("localMailboxEnabled", Boolean.TRUE);
    }
}
