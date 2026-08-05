package com.roadscanner.service.qna;

import com.roadscanner.dao.qna.AnswerDAO;
import com.roadscanner.dao.qna.QuestionDAO;
import com.roadscanner.cmn.exception.InvalidOperationException;
import com.roadscanner.cmn.exception.ResourceNotFoundException;
import com.roadscanner.cmn.exception.ConflictException;
import com.roadscanner.domain.qna.AnswerVO;
import com.roadscanner.domain.qna.QuestionCategory;
import com.roadscanner.domain.qna.QuestionVO;
import com.roadscanner.dto.AnswerResponseDTO;
import com.roadscanner.dto.AnswerSaveRequestDTO;
import com.roadscanner.dto.AnswerUpdateRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

@RequiredArgsConstructor
@Service
public class AnswerServiceImpl implements AnswerService {

    private static final int ANSWERED_CATEGORY = QuestionCategory.INQUIRY_ANSWERED;
    private static final int WAITING_CATEGORY = QuestionCategory.INQUIRY_WAITING;

    private final AnswerDAO answerDAO;
    private final QuestionDAO questionDAO;

    // 등록
    @Override
    @Transactional
    public Long save(AnswerSaveRequestDTO dto) {
        requireQuestionCategory(dto.getNo(), QuestionCategory.INQUIRY_WAITING);
        if (answerDAO.findByNo(dto.getNo()) != null) {
            throw new ConflictException("Answer already exists");
        }
        AnswerVO vo = dto.toEntity();
        Long result;
        try {
            result = answerDAO.save(vo);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Answer already exists", exception);
        }
        if (result != null && result > 0) {
            if (questionDAO.transitionCategory(
                    dto.getNo(), WAITING_CATEGORY, ANSWERED_CATEGORY) != 1) {
                throw new IllegalStateException("Question status update did not affect exactly one row");
            }
        }
        return result;
    }

    // 삭제
    @Override
    @Transactional
    public Long delete(Long no) {
        requireQuestionCategory(no, QuestionCategory.INQUIRY_ANSWERED);
        if (answerDAO.findByNo(no) == null) {
            throw new ResourceNotFoundException("Answer not found");
        }
        if (answerDAO.delete(no) != 1) {
            throw new IllegalStateException("Answer deletion did not affect exactly one row");
        }
        if (questionDAO.transitionCategory(no, ANSWERED_CATEGORY, WAITING_CATEGORY) != 1) {
            throw new IllegalStateException("Question status update did not affect exactly one row");
        }
        return no;
    }

    // 수정
    @Override
    @Transactional
    public Long update(Long no, AnswerUpdateRequestDTO dto) {
        requireQuestionCategory(no, QuestionCategory.INQUIRY_ANSWERED);
        AnswerVO vo = answerDAO.findByNo(no);
        if (vo == null) {
            throw new ResourceNotFoundException("Answer not found");
        }
        vo.update(dto.getContent());
        if (answerDAO.update(vo) != 1) {
            throw new IllegalStateException("Answer update did not affect exactly one row");
        }
        return no;

    }

    // 조회
    @Override
    public AnswerResponseDTO findByNo(Long no) {
        AnswerVO vo = answerDAO.findByNo(no);
        return vo == null ? null : new AnswerResponseDTO(vo);
    }

    private QuestionVO requireQuestionCategory(Long no, int requiredCategory) {
        QuestionVO question = questionDAO.findByNo(no);
        if (question == null) {
            throw new ResourceNotFoundException("Question not found");
        }
        if (question.getCategory() != requiredCategory) {
            throw new InvalidOperationException(
                    "Answer operation is not allowed for this question category");
        }
        return question;
    }

}
