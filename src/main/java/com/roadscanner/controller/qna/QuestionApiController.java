package com.roadscanner.controller.qna;

import com.roadscanner.cmn.AmazonS3Store;
import com.roadscanner.cmn.PcwkLogger;
import com.roadscanner.domain.UploadFile;
import com.roadscanner.dto.qna.QuestionResponseDTO;
import com.roadscanner.dto.qna.QuestionSaveRequestDTO;
import com.roadscanner.dto.qna.QuestionUpdateRequestDTO;
import com.roadscanner.service.qna.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * 질문 작성, 수정, 삭제에 관련된 클래스
 * 비동기 처리 - qna.js와 관련됨.
 */
@RequiredArgsConstructor
@RestController
public class QuestionApiController implements PcwkLogger {

    private final QuestionService questionService;
    private final AmazonS3Store fileStore;

    // 등록
    @PostMapping("/api/qna/save")
    public void save(@ModelAttribute QuestionSaveRequestDTO request) throws IOException {
        LOG.info("save 실행");
        questionService.save(request);
    }

    // 조회
    @GetMapping("/api/qna/{no}")
    public QuestionResponseDTO findByNo(@PathVariable Long no) {
        return questionService.findByNo(no);
    }

    // 수정
    @PostMapping("/api/qna/{no}")
    public Long update(@PathVariable Long no, @ModelAttribute QuestionUpdateRequestDTO request) throws IOException {
        LOG.debug("request={}", request);
        return questionService.update(no, request);
    }

    // 삭제
    @DeleteMapping("/api/qna/{no}")
    public Long delete(@PathVariable Long no) {
        return questionService.delete(no);
    }

}
