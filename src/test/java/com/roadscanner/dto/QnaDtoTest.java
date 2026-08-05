package com.roadscanner.dto;

import com.roadscanner.domain.qna.AnswerVO;
import com.roadscanner.domain.qna.QuestionVO;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class QnaDtoTest {

    @Test
    public void questionSaveRequestKeepsAttachmentId() {
        QuestionSaveRequestDTO request = new QuestionSaveRequestDTO();
        request.setCategory(30);
        request.setId("member");
        request.setIdx(77L);
        request.setTitle("제목");
        request.setContent("내용");

        QuestionVO entity = request.toEntity();

        assertThat(entity.getCategory()).isEqualTo(30);
        assertThat(entity.getId()).isEqualTo("member");
        assertThat(entity.getIdx()).isEqualTo(77L);
        assertThat(entity.getTitle()).isEqualTo("제목");
        assertThat(entity.getContent()).isEqualTo("내용");
    }

    @Test
    public void questionResponseMapsAttachmentAndFormatsDates() {
        QuestionVO question = new QuestionVO();
        question.setNo(4L);
        question.setCategory(20);
        question.setViews(3);
        question.setIdx(12L);
        question.setId("member");
        question.setTitle("제목");
        question.setContent("내용");
        question.setCreateDate(LocalDateTime.of(2026, 7, 30, 9, 8, 7));
        question.setUpdateDate(LocalDateTime.of(2026, 7, 31, 10, 9, 8));

        QuestionResponseDTO response = new QuestionResponseDTO(question);

        assertThat(response.getNo()).isEqualTo(4L);
        assertThat(response.getIdx()).isEqualTo(12L);
        assertThat(response.getCreateDate()).isEqualTo("26-07-30 09:08:07");
        assertThat(response.getUpdateDate()).isEqualTo("26-07-31 10:09:08");
    }

    @Test
    public void answerSaveRequestMapsAllFields() {
        AnswerSaveRequestDTO request = new AnswerSaveRequestDTO();
        request.setNo(4L);
        request.setId("admin");
        request.setContent("답변");

        AnswerVO entity = request.toEntity();

        assertThat(entity.getNo()).isEqualTo(4L);
        assertThat(entity.getId()).isEqualTo("admin");
        assertThat(entity.getContent()).isEqualTo("답변");
    }
}
