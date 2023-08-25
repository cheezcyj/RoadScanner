package com.roadscanner.controller.qna;

import com.roadscanner.dto.qna.AnswerResponseDTO;
import com.roadscanner.dto.qna.AnswerSaveRequestDTO;
import com.roadscanner.dto.qna.AnswerUpdateRequestDTO;
import com.roadscanner.service.qna.AnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
public class AnswerApiController {

    private final AnswerService answerService;

    // 등록
    @PostMapping("/api/qna/{no}/answer")
    public Long save(@PathVariable Long no, @RequestBody AnswerSaveRequestDTO dto) {
        return answerService.save(dto);
    }

    // 조회
    @GetMapping("/api/qna/{no}/answer")
    public AnswerResponseDTO findByNo(@PathVariable Long no) {
        return answerService.findByNo(no);
    }

    // 수정
    @PutMapping("/api/qna/{no}/answer")
    public Long update(@PathVariable Long no, @RequestBody AnswerUpdateRequestDTO dto) {
        return answerService.update(no, dto);
    }

    // 삭제
    @DeleteMapping("/api/qna/{no}/answer")
    public Long delete(@PathVariable Long no) {
        return answerService.delete(no);
    }

}
