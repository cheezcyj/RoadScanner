package com.roadscanner.service.qna;

import java.util.List;

import com.roadscanner.dto.qna.PaginationDTO;
import com.roadscanner.dto.qna.QuestionListResponseDTO;
import com.roadscanner.dto.qna.QuestionResponseDTO;
import com.roadscanner.dto.qna.QuestionSaveRequestDTO;
import com.roadscanner.dto.qna.QuestionUpdateRequestDTO;

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
