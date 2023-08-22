package com.roadscanner.service.qna;

import com.roadscanner.domain.qna.QuestionVO;
import com.roadscanner.dto.qna.*;

import java.io.IOException;
import java.util.List;

public interface QuestionService {

    List<QuestionListResponseDTO> findNotice();

    List<QuestionListResponseDTO> findAll(PaginationDTO pagination, QuestionSearchCond questionSearch);

    void save(QuestionSaveRequestDTO dto) throws IOException;

    QuestionResponseDTO findByNo(Long no);

//    List<QuestionListResponseDTO> findAll();

    Long update(Long no, QuestionUpdateRequestDTO dto) throws IOException;

    Long delete(Long no);

    // 안녕하세요. 저는 페이징을 위해 태어났어요.
//    List<QuestionListResponseDTO> findAllWithPaging(PaginationDTO pagination);

    int countQuestions(QuestionSearchCond searchCond);

    void increaseViews(Long no);

}
