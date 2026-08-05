package com.roadscanner.controller.user;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

import com.roadscanner.service.user.LocalMailSendService;

@Controller
@Profile("local")
@RequestMapping("/local/mailbox")
public class LocalMailboxController {
    private final LocalMailSendService mailService;

    @Autowired
    public LocalMailboxController(LocalMailSendService mailService) {
        this.mailService = mailService;
    }

    @GetMapping
    public String mailbox(Model model, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        model.addAttribute("messages", mailService.getMessages());
        return "local/mailbox";
    }

    @PostMapping("/clear")
    public RedirectView clear() {
        mailService.clear();
        RedirectView redirect = new RedirectView("/local/mailbox", true);
        redirect.setExposeModelAttributes(false);
        return redirect;
    }
}
