package com.roadscanner.controller.qna;

import com.roadscanner.cmn.PcwkLogger;
import com.roadscanner.dto.qna.QuestionResponseDTO;
import com.roadscanner.dto.qna.QuestionSaveRequestDTO;
import com.roadscanner.dto.qna.QuestionUpdateRequestDTO;
import com.roadscanner.service.qna.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 질문 작성, 수정, 삭제에 관련된 클래스
 * 비동기 처리 - qna.js와 관련됨.
 */
@RequiredArgsConstructor
@RestController
public class QuestionApiController implements PcwkLogger {

    private final QuestionService questionService;

    /**
     * @ModelAttribute는 model.addAttribute()를 생략시키게 해준다.
     * 검증 객체 다음에 꼭 BindingResult 객체가 와야함(순서 중요)
     * BindingReuslt는 자동으로 model에 담김.
     * @param request
     * @return
     * @throws IOException
     */
    // 등록
    @PostMapping("/api/qna/save")
    public ResponseEntity<?> save(@Validated @ModelAttribute QuestionSaveRequestDTO request, BindingResult bindingResult) throws IOException {
        LOG.debug("API SAVE 실행 = {}", request);

        // 검증이 실패하면 작성 폼으로 Ajax를 통해 에러메시지를 전송
        if (bindingResult.hasErrors()) {
            LOG.debug("error ={}", bindingResult);
            Map<String, String> errorMap = new HashMap<>();
            for (FieldError error : bindingResult.getFieldErrors()) {
                errorMap.put(error.getField(), error.getDefaultMessage());
            }

            return ResponseEntity.badRequest().body(errorMap);
        }

        questionService.save(request);
        return ResponseEntity.ok("성공");
    }

    // 조회
    @GetMapping("/api/qna/{no}")
    public QuestionResponseDTO findByNo(@PathVariable Long no) {
        return questionService.findByNo(no);
    }

    // 수정
    @PostMapping("/api/qna/{no}")
    public ResponseEntity<?> update(@PathVariable Long no, @Validated @ModelAttribute QuestionUpdateRequestDTO request, BindingResult bindingResult) throws IOException {

        LOG.debug("request={}", request);
        if (bindingResult.hasErrors()) {
            LOG.debug("errors={}", bindingResult);
            List<String> errors = bindingResult.getAllErrors().stream()
                    .map(ObjectError::getDefaultMessage)
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest().body(errors);
        }


        return ResponseEntity.ok(questionService.update(no, request));
    }
    // 삭제
    @DeleteMapping("/api/qna/{no}")
    public Long delete(@PathVariable Long no) {
        return questionService.delete(no);
    }

}
