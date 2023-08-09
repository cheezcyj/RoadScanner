package com.roadscanner.controller.qna;

import com.roadscanner.dto.QuestionResponseDTO;
import com.roadscanner.service.qna.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@RequiredArgsConstructor
@RequestMapping("/qna")
@Controller
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("questions", questionService.findAll());
        return "qna/index";
    }
    
    @GetMapping("/list")
    public String questionList() {
        return "qna/list_user";
    }
    
    @GetMapping("/listadmin")
    public String questionListA() {
        return "qna/list_admin";
    }

    @GetMapping("/write")
    public String questionWrite() {
        return "qna/question_write";
    }

    @GetMapping("/{no}")
    public String detail(@PathVariable Long no, Model model) {
        QuestionResponseDTO dto = questionService.findByNo(no);
        model.addAttribute("question", dto);
        return "qna/question-detail";
    }

    @GetMapping("/update/{no}")
    public String questionUpdate(@PathVariable Long no, Model model) {
        QuestionResponseDTO dto = questionService.findByNo(no);
        model.addAttribute("question", dto);
        return "qna/question-update";
    }

}
