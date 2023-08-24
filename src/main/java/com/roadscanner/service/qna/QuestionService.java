package com.roadscanner.service.qna;

import com.roadscanner.domain.qna.QuestionVO;
import com.roadscanner.dto.qna.*;

import java.io.IOException;
import java.util.List;

public interface QuestionService {

    List<QuestionListResponseDTO> findMyQuestion(String id, PaginationDTO pagination, QuestionSearchCond searchCond);

    int countMyQuestions(QuestionSearchCond searchCond);

    List<QuestionListResponseDTO> findNotice();

    List<QuestionListResponseDTO> findAll(PaginationDTO pagination, QuestionSearchCond questionSearch);

    void save(QuestionSaveRequestDTO dto) throws IOException;

    QuestionResponseDTO findByNo(Long no);

    Long update(Long no, QuestionUpdateRequestDTO dto) throws IOException;

    Long delete(Long no);

    int countQuestions(QuestionSearchCond searchCond);

    void increaseViews(Long no);

}
