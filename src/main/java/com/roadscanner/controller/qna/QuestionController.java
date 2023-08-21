package com.roadscanner.controller.qna;

import com.roadscanner.domain.upload.FileUploadVO;
import com.roadscanner.domain.user.MemberVO;
import com.roadscanner.dto.qna.*;
import com.roadscanner.service.qna.AnswerService;
import com.roadscanner.service.qna.QuestionService;
import com.roadscanner.service.upload.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.sql.SQLException;

@RequiredArgsConstructor
@Slf4j
@RequestMapping("/qna")
@Controller
public class QuestionController {

    private final QuestionService questionService;
    private final AnswerService answerService;

    @GetMapping
    public String index(Model model,
                        @RequestParam(defaultValue = "1") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "") String searchType,
                        @RequestParam(defaultValue = "") String keyword,
                        @RequestParam(required = false) Integer category) {
        // 페이지 번호가 유효한지 확인
        if (page < 1) {
            page = 1;
        }

        QuestionSearchCond searchCond = new QuestionSearchCond();
        searchCond.setSearchType(searchType);
        searchCond.setKeyword(keyword);
        searchCond.setCategory(category); // 카테고리 설정

        PaginationDTO pagination = new PaginationDTO(page, size);
        model.addAttribute("questions", questionService.findAll(pagination, searchCond));
//        model.addAttribute("questions", questionService.findAllWithPaging(pagination));

        int totalQuestions = questionService.countQuestions(searchCond);
        int totalPages = (int) Math.ceil((double) totalQuestions / size);

        // 총 페이지 수가 0이면, 페이지 번호도 0으로 설정
//        if (totalPages == 0) {
//            page = 0;
//        }
        if (totalQuestions == 0) {
            totalPages = 0;
            page = 0;
        }
        // 현재 페이지 번호가 총 페이지 수보다 크면, 현재 페이지 번호를 총 페이지 수로 설정
        else if (page > totalPages) {
            page = totalPages;
        }

        model.addAttribute("totalPages", totalPages);
        model.addAttribute("page", page);

        return "qna/question-index";
    }

    /**
     * 로그인 한 유저는 세션에 저장되어 있음.
     * memberVO 변수로 값을 받고, 모델로 View에 전달
     * @param memberVO
     * @param model
     * @return
     */
    @GetMapping("/save")
    public String QuestionSave(@SessionAttribute("user") MemberVO memberVO, Model model) {
        model.addAttribute("user", memberVO); // 모델에 사용자 정보 추가
        return "qna/question-save";
    }

    @GetMapping("/{no}")
    public String detail(@PathVariable Long no, Model model, @SessionAttribute("user") MemberVO memberVO) {
        // 조회수 증가
        questionService.increaseViews(no);

        QuestionResponseDTO dto = questionService.findByNo(no);
        model.addAttribute("question", dto);

        // 답변 등록 결과를 반환값으로 받아서 이용
        AnswerResponseDTO answerDto = answerService.findByNo(no);
        model.addAttribute("answer", answerDto);
        model.addAttribute("user", memberVO); // 현재 로그인한 사용자 정보를 모델에 추가
        // model.addAttribute("answerId", memberVO.getId());

        return "qna/question-detail";
    }

    @GetMapping("/update/{no}")
    public String questionUpdate(@PathVariable Long no, Model model) {
        QuestionResponseDTO dto = questionService.findByNo(no);
        model.addAttribute("question", dto);
        return "qna/question-update";
    }
}
