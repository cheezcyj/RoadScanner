package com.roadscanner.service.qna;

import com.roadscanner.dao.qna.AnswerDAO;
import com.roadscanner.dao.qna.QuestionDAO;
import com.roadscanner.domain.qna.AnswerVO;
import com.roadscanner.dto.AnswerResponseDTO;
import com.roadscanner.dto.AnswerSaveRequestDTO;
import com.roadscanner.dto.AnswerUpdateRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AnswerServiceImpl implements AnswerService {

    private final AnswerDAO answerDAO;
    private final QuestionDAO questionDAO;

    // 등록
    @Override
    public Long save(AnswerSaveRequestDTO dto) {
        AnswerVO vo = dto.toEntity();
        return answerDAO.save(vo);
    }

    // 삭제
    @Override
    public Long delete(Long no) {
        answerDAO.delete(no);
        return no;
    }

    // 수정
    @Override
    public Long update(Long no, AnswerUpdateRequestDTO dto) {
        AnswerVO vo = answerDAO.findByNo(no);
        vo.update(dto.getContent());
        answerDAO.update(vo);
        // System.out.println(vo);
        return no;

    }

    // 조회
    @Override
    public AnswerResponseDTO findByNo(Long no) {
        AnswerVO vo = (AnswerVO) answerDAO.findByNo(no);
        return new AnswerResponseDTO(vo);
    }

}
