package com.roadscanner.controller.qna;

import com.roadscanner.dto.PaginationDTO;
import com.roadscanner.dto.QuestionResponseDTO;
import com.roadscanner.service.qna.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequiredArgsConstructor
@RequestMapping("/qna")
@Controller
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    public String index(Model model,
                        @RequestParam(defaultValue = "1") int page,
                        @RequestParam(defaultValue = "10") int size) {
        // 페이지 번호가 유효한지 확인
        if (page < 1) {
            page = 1;
        }

        PaginationDTO pagination = new PaginationDTO(page, size);
        model.addAttribute("questions", questionService.findAllWithPaging(pagination));

        int totalQuestions = questionService.countQuestions();
        int totalPages = (int) Math.ceil((double) totalQuestions / size);

        // 총 페이지 수가 0이면, 페이지 번호도 0으로 설정
        if (totalPages == 0) {
            page = 0;
        }
        // 현재 페이지 번호가 총 페이지 수보다 크면, 현재 페이지 번호를 총 페이지 수로 설정
        else if (page > totalPages) {
            page = totalPages;
        }

        model.addAttribute("totalPages", totalPages);
        model.addAttribute("page", page);

        return "qna/index";
    }

    @GetMapping("/save")
    public String QuestionSave() {
        return "qna/question-save";
    }

    @GetMapping("/{no}")
    public String detail(@PathVariable Long no, Model model) {
        // 조회수 증가
        questionService.increaseViews(no);

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
    
    @GetMapping("/main")
    public String QuestionMain() {
        return "qna/main";
    }
    
    @GetMapping("/listtest")
    public String QuestionListT() {
        return "qna/list_test";
    }
    
    @GetMapping("/list")
    public String QuestionList() {
        return "qna/list";
    }
    
    @GetMapping("/listadmin")
    public String QuestionListA() {
        return "qna/list_admin";
    }
    
    @GetMapping("/write")
    public String QuestionWrite() {
        return "qna/write";
    }
    
    @GetMapping("/writeadmin")
    public String QuestionWriteA() {
        return "qna/write_admin";
    }

}
