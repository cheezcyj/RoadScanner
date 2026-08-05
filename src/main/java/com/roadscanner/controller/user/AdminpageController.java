package com.roadscanner.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;

import com.roadscanner.domain.user.MemberListPage;
import com.roadscanner.domain.user.MemberVO;
import com.roadscanner.service.user.AdminService;

@Controller
@RequestMapping("/login")
public class AdminpageController {
    private static final int MAX_KEYWORD_LENGTH = 100;

    private final AdminService service;

    @Autowired
    public AdminpageController(AdminService service) {
        this.service = service;
    }

    @GetMapping("/list_member")
    public String listMembers(Model model,
            @RequestParam(value = "num", defaultValue = "1") int requestedPage,
            @RequestParam(value = "keyword", defaultValue = "") String rawKeyword) throws Exception {
        String keyword = normalizeKeyword(rawKeyword);
        MemberListPage page = new MemberListPage(
                requestedPage, service.member_searchCntBox(keyword), keyword);

        model.addAttribute("list", service.member(page.getOffset(), page.getPageSize(), keyword));
        model.addAttribute("memberPage", page);
        return "login/list_member";
    }

    @GetMapping("/list_admin")
    public String listAdministrators(Model model,
            @RequestParam(value = "num", defaultValue = "1") int requestedPage,
            @RequestParam(value = "keyword", defaultValue = "") String rawKeyword,
            @SessionAttribute("user") MemberVO currentAdministrator) throws Exception {
        String keyword = normalizeKeyword(rawKeyword);
        String excludedId = currentAdministrator.getId();
        MemberListPage page = new MemberListPage(
                requestedPage, service.admin_searchCntBox(keyword, excludedId), keyword);

        model.addAttribute("list",
                service.admin(page.getOffset(), page.getPageSize(), keyword, excludedId));
        model.addAttribute("adminPage", page);
        return "login/list_admin";
    }

    @GetMapping("/list_banned")
    public String listBannedMembers(Model model,
            @RequestParam(value = "num", defaultValue = "1") int requestedPage,
            @RequestParam(value = "keyword", defaultValue = "") String rawKeyword) throws Exception {
        String keyword = normalizeKeyword(rawKeyword);
        MemberListPage page = new MemberListPage(
                requestedPage, service.banned_searchCntBox(keyword), keyword);

        model.addAttribute("list", service.banned(page.getOffset(), page.getPageSize(), keyword));
        model.addAttribute("bannedPage", page);
        return "login/list_banned";
    }

    private String normalizeKeyword(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim();
        return normalized.length() <= MAX_KEYWORD_LENGTH
                ? normalized
                : normalized.substring(0, MAX_KEYWORD_LENGTH);
    }
}
