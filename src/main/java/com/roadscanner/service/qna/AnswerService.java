package com.roadscanner.service.qna;

import com.roadscanner.dto.qna.AnswerResponseDTO;
import com.roadscanner.dto.qna.AnswerSaveRequestDTO;
import com.roadscanner.dto.qna.AnswerUpdateRequestDTO;

public interface AnswerService {
    // 답변 등록
    Long save(AnswerSaveRequestDTO dto);

    // 삭제
    Long delete(Long no);

    // 수정
    Long update(Long no, AnswerUpdateRequestDTO dto);

    // 조회
    AnswerResponseDTO findByNo(Long no);

}
