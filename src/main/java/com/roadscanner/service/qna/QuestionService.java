package com.roadscanner.service.qna;

import com.roadscanner.dto.QuestionResponseDTO;
import com.roadscanner.dto.QuestionSaveRequestDTO;
import com.roadscanner.dto.QuestionListResponseDTO;
import com.roadscanner.dto.QuestionUpdateRequestDTO;

import java.util.List;

public interface QuestionService{

    /**
     * 게시글 작성
     */
    Long save(QuestionSaveRequestDTO dto);

    QuestionResponseDTO findByNo(Long no);

    List<QuestionListResponseDTO> findAll();

    Long update(Long no, QuestionUpdateRequestDTO dto);

    Long delete(Long no);


}
