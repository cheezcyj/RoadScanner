package com.roadscanner.service.qna;

import com.roadscanner.dao.qna.AnswerDAO;
import com.roadscanner.dao.qna.QuestionDAO;
import com.roadscanner.domain.qna.AnswerVO;
import com.roadscanner.dto.qna.AnswerResponseDTO;
import com.roadscanner.dto.qna.AnswerSaveRequestDTO;
import com.roadscanner.dto.qna.AnswerUpdateRequestDTO;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AnswerServiceImpl implements AnswerService {

    private final AnswerDAO answerDAO;
    private final QuestionDAO questionDAO;

    private Logger LOG = LogManager.getLogger(getClass());

    // 등록
    @Override
    public Long save(AnswerSaveRequestDTO dto) {
        // 답변 저장
        AnswerVO vo = dto.toEntity();
        Long savedCount = answerDAO.save(vo);

        // 저장이 성공했을 경우에만 카테고리 업데이트
        if(savedCount > 0) {
            questionDAO.updateCategory(vo.getNo());
        }

        return savedCount;
    }

    // 삭제
    @Override
    public Long delete(Long no) {

        AnswerVO vo = answerDAO.findByNo(no);

        if(vo != null) {
            // 답변 삭제
            answerDAO.delete(no);

            // 카테고리 업데이트
            questionDAO.updateCategory(no);
            return no;

        } else {
            return null;
        }

    }

    // 수정
    @Override
    public Long update(Long no, AnswerUpdateRequestDTO dto) {
        AnswerVO vo = answerDAO.findByNo(no);
        vo.update(dto.getContent());
        answerDAO.update(vo);
        return no;

    }

    // 조회
    @Override
    public AnswerResponseDTO findByNo(Long no) {
        AnswerVO vo = (AnswerVO) answerDAO.findByNo(no);
        if (vo != null) {
            // 답변이 없는 경우 빈 객체 또는 null을 반환
            return new AnswerResponseDTO(vo);
        } else {
            return null;
        }
    }

}
