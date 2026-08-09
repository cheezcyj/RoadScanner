package com.roadscanner.service.qna;

import com.roadscanner.dao.qna.AnswerDAO;
import com.roadscanner.dao.qna.QuestionDAO;
import com.roadscanner.domain.qna.AnswerVO;
import com.roadscanner.domain.qna.QuestionVO;
import com.roadscanner.dto.AnswerResponseDTO;
import com.roadscanner.dto.AnswerSaveRequestDTO;
import com.roadscanner.dto.AnswerUpdateRequestDTO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AnswerServiceImplTest {

    @Mock
    private AnswerDAO answerDAO;

    @Mock
    private QuestionDAO questionDAO;

    @InjectMocks
    private AnswerServiceImpl answerService;

    @Test
    public void saveConvertsAndPersistsAnswer() {
        AnswerSaveRequestDTO request = new AnswerSaveRequestDTO();
        request.setNo(10L);
        request.setId("admin");
        request.setContent("답변 내용");
        when(questionDAO.findByNo(10L)).thenReturn(question(10L, 30));
        when(answerDAO.save(any(AnswerVO.class))).thenReturn(1L);
        when(questionDAO.transitionCategory(10L, 30, 20)).thenReturn(1);

        Long result = answerService.save(request);

        ArgumentCaptor<AnswerVO> captor = ArgumentCaptor.forClass(AnswerVO.class);
        verify(answerDAO).save(captor.capture());
        assertThat(result).isEqualTo(1L);
        assertThat(captor.getValue().getNo()).isEqualTo(10L);
        assertThat(captor.getValue().getId()).isEqualTo("admin");
        assertThat(captor.getValue().getContent()).isEqualTo("답변 내용");
        verify(questionDAO).transitionCategory(10L, 30, 20);
    }

    @Test
    public void findByNoReturnsNullWhenAnswerDoesNotExist() {
        when(answerDAO.findByNo(10L)).thenReturn(null);

        AnswerResponseDTO result = answerService.findByNo(10L);

        assertThat(result).isNull();
    }

    @Test
    public void findByNoMapsExistingAnswer() {
        LocalDateTime created = LocalDateTime.of(2026, 7, 31, 11, 0);
        AnswerVO answer = new AnswerVO(10L, "admin", "답변 내용");
        answer.setCreateDate(created);
        answer.setUpdateDate(null);
        when(answerDAO.findByNo(10L)).thenReturn(answer);

        AnswerResponseDTO result = answerService.findByNo(10L);

        assertThat(result.getId()).isEqualTo("admin");
        assertThat(result.getContent()).isEqualTo("답변 내용");
        assertThat(result.getCreateDate()).isEqualTo(created);
        assertThat(result.getUpdateDate()).isNull();
    }

    @Test
    public void updateChangesPersistedAnswer() {
        when(questionDAO.findByNo(10L)).thenReturn(question(10L, 20));
        AnswerVO answer = new AnswerVO(10L, "admin", "기존 답변");
        AnswerUpdateRequestDTO request = new AnswerUpdateRequestDTO("수정 답변");
        when(answerDAO.findByNo(10L)).thenReturn(answer);
        when(answerDAO.update(any(AnswerVO.class))).thenReturn(1);

        Long result = answerService.update(10L, request);

        assertThat(result).isEqualTo(10L);
        assertThat(answer.getContent()).isEqualTo("수정 답변");
        verify(answerDAO).update(answer);
    }

    @Test
    public void deleteDelegatesToDao() {
        when(questionDAO.findByNo(10L)).thenReturn(question(10L, 20));
        when(answerDAO.findByNo(10L)).thenReturn(new AnswerVO(10L, "admin", "답변"));
        when(answerDAO.delete(10L)).thenReturn(1);
        when(questionDAO.transitionCategory(10L, 20, 30)).thenReturn(1);

        assertThat(answerService.delete(10L)).isEqualTo(10L);

        verify(answerDAO).delete(10L);
        verify(questionDAO).transitionCategory(10L, 20, 30);
    }

    @Test
    public void failedSaveDoesNotChangeQuestionStatus() {
        AnswerSaveRequestDTO request = new AnswerSaveRequestDTO();
        request.setNo(10L);
        request.setId("admin");
        request.setContent("답변 내용");
        when(questionDAO.findByNo(10L)).thenReturn(question(10L, 30));
        when(answerDAO.save(any(AnswerVO.class))).thenReturn(0L);

        assertThat(answerService.save(request)).isZero();

        verify(questionDAO, never()).transitionCategory(10L, 30, 20);
    }

    @Test
    public void noticeCannotReceiveAnswer() {
        AnswerSaveRequestDTO request = new AnswerSaveRequestDTO();
        request.setNo(10L);
        when(questionDAO.findByNo(10L)).thenReturn(question(10L, 10));

        assertThatThrownBy(() -> answerService.save(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("category");

        verify(answerDAO, never()).save(any(AnswerVO.class));
    }

    @Test
    public void publicQnaCannotReceiveAnAdministrativeAnswer() {
        AnswerSaveRequestDTO request = new AnswerSaveRequestDTO();
        request.setNo(10L);
        when(questionDAO.findByNo(10L)).thenReturn(question(10L, 40));

        assertThatThrownBy(() -> answerService.save(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("category");

        verify(answerDAO, never()).save(any(AnswerVO.class));
        verify(questionDAO, never()).transitionCategory(10L, 30, 20);
    }

    @Test
    public void onlyAnsweredInquiryCanUpdateAnAnswer() {
        when(questionDAO.findByNo(10L)).thenReturn(question(10L, 30));

        assertThatThrownBy(() -> answerService.update(
                10L, new AnswerUpdateRequestDTO("updated")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("category");

        verify(answerDAO, never()).findByNo(10L);
        verify(answerDAO, never()).update(any(AnswerVO.class));
    }

    @Test
    public void publicQnaCannotEnterAnswerDeletionWorkflow() {
        when(questionDAO.findByNo(10L)).thenReturn(question(10L, 40));

        assertThatThrownBy(() -> answerService.delete(10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("category");

        verify(answerDAO, never()).findByNo(10L);
        verify(answerDAO, never()).delete(10L);
        verify(questionDAO, never()).transitionCategory(10L, 20, 30);
    }

    @Test
    public void deletingMissingAnswerDoesNotChangeQuestionStatus() {
        when(questionDAO.findByNo(10L)).thenReturn(question(10L, 20));
        when(answerDAO.findByNo(10L)).thenReturn(null);

        assertThatThrownBy(() -> answerService.delete(10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");

        verify(questionDAO, never()).transitionCategory(10L, 20, 30);
    }

    @Test
    public void zeroAffectedRowsAreNotReportedAsSuccessfulMutations() {
        when(questionDAO.findByNo(10L)).thenReturn(question(10L, 20));
        AnswerVO answer = new AnswerVO(10L, "admin", "기존 답변");
        when(answerDAO.findByNo(10L)).thenReturn(answer);
        when(answerDAO.update(any(AnswerVO.class))).thenReturn(0);

        assertThatThrownBy(() -> answerService.update(
                10L, new AnswerUpdateRequestDTO("수정 답변")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exactly one row");
    }

    @Test
    public void duplicateAnswerIsRejectedWithoutChangingQuestionStatus() {
        AnswerSaveRequestDTO request = new AnswerSaveRequestDTO();
        request.setNo(10L);
        request.setId("admin");
        request.setContent("답변 내용");
        when(questionDAO.findByNo(10L)).thenReturn(question(10L, 30));
        when(answerDAO.findByNo(10L)).thenReturn(new AnswerVO(10L, "admin", "기존 답변"));

        assertThatThrownBy(() -> answerService.save(request))
                .hasMessageContaining("already exists");

        verify(answerDAO, never()).save(any(AnswerVO.class));
        verify(questionDAO, never()).transitionCategory(10L, 30, 20);
    }

    private QuestionVO question(Long no, int category) {
        QuestionVO question = new QuestionVO();
        question.setNo(no);
        question.setCategory(category);
        return question;
    }
}
