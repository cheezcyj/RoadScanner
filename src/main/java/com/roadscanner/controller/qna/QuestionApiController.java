package com.roadscanner.controller.qna;

import com.roadscanner.dto.QuestionResponseDTO;
import com.roadscanner.dto.QuestionSaveRequestDTO;
import com.roadscanner.dto.QuestionUpdateRequestDTO;
import com.roadscanner.service.qna.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
public class QuestionApiController {

    private final QuestionService questionService;

    // 등록
    @PostMapping("/api/qna/save")
    public Long save(@RequestBody QuestionSaveRequestDTO dto) {
        return questionService.save(dto);
    }

    // 조회
    @GetMapping("/api/qna/{no}")
    public QuestionResponseDTO findByNo(@PathVariable Long no) {
        return questionService.findByNo(no);
    }

    // 수정
    @PutMapping("/api/qna/{no}")
    public Long update(@PathVariable Long no, @RequestBody QuestionUpdateRequestDTO dto) {
        return questionService.update(no, dto);
    }

    // 삭제
    @DeleteMapping("/api/qna/{no}")
    public Long delete(@PathVariable Long no) {
        return questionService.delete(no);
    }

}
