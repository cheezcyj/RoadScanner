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
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RequestMapping("/qna")
@Controller
public class QuestionController {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;

    private final QuestionService questionService;
    private final AnswerService answerService;

    @GetMapping
    public String index(Model model,
                        @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
                        @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
                        @RequestParam(defaultValue = "") String searchType,
                        @RequestParam(defaultValue = "") String keyword,
                        @RequestParam(required = false) Integer category,
                        @SessionAttribute("user") MemberVO memberVO) {

        QuestionSearchCond searchCond = createSearchCond(searchType, keyword, category);
        int totalQuestions = questionService.countQuestions(searchCond);
        PaginationDTO pagination = initializePagination(page, size, totalQuestions, model);

        model.addAttribute("questions", questionService.findAll(pagination, searchCond));
        model.addAttribute("searchType", searchType);
        model.addAttribute("keyword", keyword);
        model.addAttribute("category", category);

        List<QuestionListResponseDTO> notice = questionService.findNotice();
        model.addAttribute("notice", notice);

        model.addAttribute("user", memberVO);

        return "qna/index";
    }

    @GetMapping("/my")
    public String findMyQuestion(Model model,
                                 @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
                                 @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
                                 @RequestParam(defaultValue = "") String searchType,
                                 @RequestParam(defaultValue = "") String keyword,
                                 @RequestParam(required = false) Integer category,
                                 @SessionAttribute("user") MemberVO memberVO) {

        QuestionSearchCond searchCond = createSearchCond(searchType, keyword, category);
        searchCond.setId(memberVO.getId());  // user 정보 설정

        int totalQuestions = questionService.countMyQuestions(searchCond);
        PaginationDTO pagination = initializePagination(page, size, totalQuestions, model);

        // user 정보 추가
        String id = memberVO.getId();

        model.addAttribute("questions", questionService.findMyQuestion(id, pagination, searchCond));
        model.addAttribute("searchType", searchType);
        model.addAttribute("keyword", keyword);
        model.addAttribute("category", category);

        return "qna/my-question";
    }

    /**
     * 검색 분류, 키워드, 카테고리 set을 위한 메서드
     * @param searchType
     * @param keyword
     * @param category
     * @return
     */
    private QuestionSearchCond createSearchCond(String searchType, String keyword, Integer category) {
        QuestionSearchCond searchCond = new QuestionSearchCond();
        searchCond.setSearchType(searchType);
        searchCond.setKeyword(keyword);
        searchCond.setCategory(category);
        return searchCond;
    }

    /**
     * 페이징 설정 메서드
     * @param page
     * @param size
     * @param totalQuestions
     * @param model
     * @return
     */
    private PaginationDTO initializePagination(int page, int size, int totalQuestions, Model model) {
        if (page < 1) {
            page = DEFAULT_PAGE;
        }

        if (totalQuestions == 0) {
            page = 0;
        }

        PaginationDTO pagination = new PaginationDTO(page, size, totalQuestions);

        if (page > pagination.getTotalPage()) {
            page = pagination.getTotalPage();
        }

        model.addAttribute("pagination", pagination);
        model.addAttribute("page", page);

        return pagination;
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

        return "qna/question-detail";
    }

    @GetMapping("/update/{no}")
    public String questionUpdate(@PathVariable Long no, Model model) {
        QuestionResponseDTO dto = questionService.findByNo(no);
        model.addAttribute("question", dto);
        return "qna/question-update";
    }
}
