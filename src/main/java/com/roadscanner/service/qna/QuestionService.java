package com.roadscanner.service.qna;

import com.roadscanner.dto.*;

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

    // 안녕하세요. 저는 페이징을 위해 태어났어요.
    List<QuestionListResponseDTO> findAllWithPaging(PaginationDTO pagination);

    int countQuestions();

    void increaseViews(Long no);

}
