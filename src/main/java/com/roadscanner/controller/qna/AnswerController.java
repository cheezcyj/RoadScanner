package com.roadscanner.controller.qna;

import com.roadscanner.dto.AnswerResponseDTO;
import com.roadscanner.dto.QuestionResponseDTO;
import com.roadscanner.service.qna.AnswerService;
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
public class AnswerController {

    private final AnswerService answerService;
    private final QuestionService questionService;




}
