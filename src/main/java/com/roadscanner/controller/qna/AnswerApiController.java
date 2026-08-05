package com.roadscanner.controller.qna;

import com.roadscanner.dto.AnswerResponseDTO;
import com.roadscanner.dto.AnswerSaveRequestDTO;
import com.roadscanner.dto.AnswerUpdateRequestDTO;
import com.roadscanner.dto.QuestionResponseDTO;
import com.roadscanner.domain.qna.QuestionCategory;
import com.roadscanner.domain.user.MemberVO;
import com.roadscanner.service.qna.AnswerService;
import com.roadscanner.service.qna.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;

@RequiredArgsConstructor
@RestController
public class AnswerApiController {

    private static final int ADMIN_GRADE = 2;

    private final AnswerService answerService;
    private final QuestionService questionService;

    // 등록
    @PostMapping("/api/qna/{no}/answer")
    public Long save(@PathVariable Long no, @Valid @RequestBody AnswerSaveRequestDTO dto,
                     @SessionAttribute("user") MemberVO user) {
        assertAdmin(user);
        dto.setNo(no);
        dto.setId(user.getId());
        return answerService.save(dto);
    }

    // 조회
    @GetMapping("/api/qna/{no}/answer")
    public AnswerResponseDTO findByNo(@PathVariable Long no,
                                      @SessionAttribute("user") MemberVO user) {
        QuestionResponseDTO question = questionService.findByNo(no);
        if (question == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found");
        }
        assertCanViewInquiry(question, user);
        AnswerResponseDTO answer = answerService.findByNo(no);
        if (answer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Answer not found");
        }
        return answer;
    }

    @PutMapping("/api/qna/{no}/answer")
    public Long update(@PathVariable Long no, @Valid @RequestBody AnswerUpdateRequestDTO dto,
                       @SessionAttribute("user") MemberVO user) {
        assertAdmin(user);
        return answerService.update(no, dto);
    }

    @DeleteMapping("/api/qna/{no}/answer")
    public Long delete(@PathVariable Long no, @SessionAttribute("user") MemberVO user) {
        assertAdmin(user);
        return answerService.delete(no);
    }

    private void assertAdmin(MemberVO user) {
        if (user.getGrade() != ADMIN_GRADE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator access required");
        }
    }

    private void assertCanViewInquiry(QuestionResponseDTO question, MemberVO user) {
        if (!QuestionCategory.isInquiry(question.getCategory())
                || (user.getGrade() != ADMIN_GRADE
                    && !question.getId().equals(user.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Inquiry access denied");
        }
    }
}
