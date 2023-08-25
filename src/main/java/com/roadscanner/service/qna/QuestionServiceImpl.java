package com.roadscanner.service.qna;

import com.roadscanner.dao.qna.QuestionDAO;
import com.roadscanner.domain.qna.QuestionVO;
import com.roadscanner.dto.qna.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class QuestionServiceImpl implements QuestionService {

    private final QuestionDAO questionDAO;

    @Override
    public List<QuestionListResponseDTO> findAll(PaginationDTO pagination, QuestionSearchCond questionSearch) {
        List<QuestionVO> questionList = questionDAO.findAll(pagination, questionSearch);
        List<QuestionListResponseDTO> dto = new ArrayList<>();

        for (QuestionVO question : questionList) {
            dto.add(new QuestionListResponseDTO(question));
        }

        return dto;
    }

    @Override
    public Long save(QuestionSaveRequestDTO dto) {

        // DTO(사용자가 제공한 정보를 통해) 질문 VO 객체 생성
        QuestionVO vo = dto.toEntity();
        // DAO를 통해 데이터베이스에 질문 등록 잘지내시나요?
        return questionDAO.save(vo);
    }

    @Override
    public QuestionResponseDTO findByNo(Long no) {
        QuestionVO vo = (QuestionVO) questionDAO.findByNo(no);
        return new QuestionResponseDTO(vo);
    }

    @Override
    public Long update(Long no, QuestionUpdateRequestDTO dto) {
        // findById 메서드를 완성 시켜야함 단건 조회후 수정
        QuestionVO vo = questionDAO.findByNo(no);
        vo.update(dto.getCategory(), dto.getTitle(), dto.getIdx(), dto.getContent());
        questionDAO.update(vo);
        return no;
    }
    @Override
    public Long delete(Long no) {
        questionDAO.delete(no);
        return no;
    }

    @Override
    public int countQuestions(QuestionSearchCond searchCond) {
        return questionDAO.countQuestions(searchCond);
    }

    @Override
    public void increaseViews(Long no) {
        questionDAO.increaseViews(no);
    }

    // 게시글 분류(category) 변경
    @Override
    public Long updateCategory(Long no) {
        questionDAO.updateCategory(no);
        return no;
    }
}
