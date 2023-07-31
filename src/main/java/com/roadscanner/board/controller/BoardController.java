package com.roadscanner.board.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/board")
public class BoardController {
	
	@GetMapping("/")
	public String boardView() {
		return "/board/list_user";
	}

    @GetMapping("/write")
    public String writeView() {
        return "/board/write";
    }
    
    @GetMapping("/detail")
    public String detailView() {
    	return "/board/post_detail";
    }
    
    @GetMapping("/admin")
    public String boardAdminView() {
    	return "/board/list_admin";
    }
    
    @GetMapping("/admin/write")
    public String adminPostWrite() {
        return "/board/admin-post-write";
        
    }
}
