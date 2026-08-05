package com.roadscanner.controller.user;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@Profile("local")
public class LocalMailboxModelAdvice {

    @ModelAttribute("localMailboxEnabled")
    public boolean localMailboxEnabled() {
        return true;
    }
}
