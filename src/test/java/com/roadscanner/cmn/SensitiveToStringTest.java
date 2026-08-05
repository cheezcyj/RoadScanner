package com.roadscanner.cmn;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.Test;

import com.roadscanner.domain.qna.QuestionVO;
import com.roadscanner.domain.result.ResultImgVO;
import com.roadscanner.domain.upload.FileUploadVO;
import com.roadscanner.dto.AnswerSaveRequestDTO;
import com.roadscanner.dto.AnswerUpdateRequestDTO;
import com.roadscanner.dto.QuestionListResponseDTO;
import com.roadscanner.dto.QuestionSaveRequestDTO;
import com.roadscanner.dto.QuestionUpdateRequestDTO;

public class SensitiveToStringTest {

    @Test
    public void commonValueObjectsRedactUserControlledValues() {
        DTO search = new DTO();
        search.setSearchWord("private-search-term");
        search.setSearchDiv("private-search-type");

        MessageVO message = new MessageVO("20", "member@example.test");
        ResultImgVO result = new ResultImgVO(1, "private-image-name", "private-result", "https://example.test/private");
        FileUploadVO upload = new FileUploadVO(1, "private-member-id", 10, "2026-08-01",
                "private-file-name", "https://example.test/upload", 100, 0, 0, 0, 0);

        assertRedacted(search, "private-search-term", "private-search-type");
        assertRedacted(message, "member@example.test");
        assertRedacted(result, "private-image-name", "private-result", "https://example.test/private");
        assertRedacted(upload, "private-member-id", "private-file-name", "https://example.test/upload");
    }

    @Test
    public void qnaRequestAndResponseObjectsDoNotPrintUserContent() {
        AnswerSaveRequestDTO answerSave = new AnswerSaveRequestDTO();
        answerSave.setId("private-admin-id");
        answerSave.setContent("private-answer-content");

        AnswerUpdateRequestDTO answerUpdate = new AnswerUpdateRequestDTO("private-updated-answer");

        QuestionSaveRequestDTO questionSave = new QuestionSaveRequestDTO();
        questionSave.setId("private-writer-id");
        questionSave.setTitle("private-question-title");
        questionSave.setContent("private-question-content");

        QuestionUpdateRequestDTO questionUpdate = new QuestionUpdateRequestDTO(
                10, "private-updated-title", 1L, "private-updated-content");

        QuestionVO question = new QuestionVO();
        question.setNo(1L);
        question.setCategory(10);
        question.setViews(0);
        question.setId("private-list-writer");
        question.setTitle("private-list-title");
        question.setCreateDate(LocalDateTime.of(2026, 8, 1, 12, 0));
        QuestionListResponseDTO questionList = new QuestionListResponseDTO(question);

        assertRedacted(answerSave, "private-admin-id", "private-answer-content");
        assertRedacted(answerUpdate, "private-updated-answer");
        assertRedacted(questionSave, "private-writer-id", "private-question-title", "private-question-content");
        assertRedacted(questionUpdate, "private-updated-title", "private-updated-content");
        assertRedacted(questionList, "private-list-writer", "private-list-title");
    }

    private void assertRedacted(Object value, String... sensitiveValues) {
        for (String sensitiveValue : sensitiveValues) {
            assertThat(value.toString()).doesNotContain(sensitiveValue);
        }
    }
}
