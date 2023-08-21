package com.roadscanner.service.qna;

import com.roadscanner.domain.qna.QuestionVO;
import com.roadscanner.dto.qna.*;

import java.io.IOException;
import java.util.List;

public interface QuestionService{

    /**
     * 게시글 작성
     */

    List<QuestionListResponseDTO> findAll(PaginationDTO pagination, QuestionSearchCond questionSearch);

    void save(QuestionSaveRequestDTO dto) throws IOException;

    QuestionResponseDTO findByNo(Long no);

//    List<QuestionListResponseDTO> findAll();

    Long update(Long no, QuestionUpdateRequestDTO dto);

    Long delete(Long no);

    // 안녕하세요. 저는 페이징을 위해 태어났어요.
//    List<QuestionListResponseDTO> findAllWithPaging(PaginationDTO pagination);

    int countQuestions(QuestionSearchCond searchCond);

    void increaseViews(Long no);

    // 질문글 분류 변경
//    Long updateCategory(Long no);

}
