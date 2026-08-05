package com.roadscanner.controller.qna;

import com.roadscanner.dto.AnswerSaveRequestDTO;
import com.roadscanner.dto.AnswerUpdateRequestDTO;
import com.roadscanner.dto.QuestionResponseDTO;
import com.roadscanner.domain.user.MemberVO;
import com.roadscanner.service.qna.AnswerService;
import com.roadscanner.service.qna.QuestionService;
import com.roadscanner.cmn.exception.InvalidOperationException;
import com.roadscanner.cmn.exception.ResourceNotFoundException;
import com.roadscanner.cmn.exception.ConflictException;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class AnswerApiControllerTest {

    @Mock
    private AnswerService answerService;

    @Mock
    private QuestionService questionService;

    @InjectMocks
    private AnswerApiController controller;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void saveUsesQuestionNumberFromPath() {
        AnswerSaveRequestDTO request = new AnswerSaveRequestDTO();
        request.setNo(999L);
        request.setId("admin");
        request.setContent("답변");
        when(answerService.save(request)).thenReturn(12L);
        MemberVO admin = member("real-admin", 2);

        Long result = controller.save(12L, request, admin);

        ArgumentCaptor<AnswerSaveRequestDTO> captor = ArgumentCaptor.forClass(AnswerSaveRequestDTO.class);
        verify(answerService).save(captor.capture());
        assertThat(result).isEqualTo(12L);
        assertThat(captor.getValue().getNo()).isEqualTo(12L);
        assertThat(captor.getValue().getId()).isEqualTo("real-admin");
    }

    @Test
    public void updateAndDeleteDelegatePathNumber() {
        AnswerUpdateRequestDTO request = new AnswerUpdateRequestDTO("수정 답변");
        when(answerService.update(7L, request)).thenReturn(7L);
        when(answerService.delete(7L)).thenReturn(7L);
        MemberVO admin = member("admin", 2);

        assertThat(controller.update(7L, request, admin)).isEqualTo(7L);
        assertThat(controller.delete(7L, admin)).isEqualTo(7L);

        verify(answerService).update(7L, request);
        verify(answerService).delete(7L);
    }

    @Test
    public void regularMemberCannotMutateAnswers() {
        MemberVO member = member("member", 1);

        assertThatThrownBy(() -> controller.delete(7L, member))
                .hasMessageContaining("403 FORBIDDEN");
    }

    @Test
    public void htmlOnlyAnswerReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/qna/7/answer")
                        .sessionAttr("user", member("admin", 2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"<p>&nbsp;</p>\"}"))
                .andExpect(status().isBadRequest());

        verify(answerService, never()).save(any(AnswerSaveRequestDTO.class));
    }

    @Test
    public void missingAnswerReturnsNotFound() throws Exception {
        QuestionResponseDTO inquiry = question("owner", 30);
        when(questionService.findByNo(404L)).thenReturn(inquiry);
        when(answerService.findByNo(404L)).thenReturn(null);

        mockMvc.perform(get("/api/qna/404/answer")
                        .sessionAttr("user", member("owner", 1)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void answerReadIsLimitedToInquiryOwnerOrAdministrator() throws Exception {
        QuestionResponseDTO inquiry = question("owner", 30);
        when(questionService.findByNo(7L)).thenReturn(inquiry);

        mockMvc.perform(get("/api/qna/7/answer")
                        .sessionAttr("user", member("other", 1)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/qna/7/answer")
                        .sessionAttr("user", member("owner", 1)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/qna/7/answer")
                        .sessionAttr("user", member("admin", 2)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void boardPostHasNoAnswerReadEndpoint() throws Exception {
        QuestionResponseDTO boardPost = question("owner", 40);
        when(questionService.findByNo(8L)).thenReturn(boardPost);

        mockMvc.perform(get("/api/qna/8/answer")
                        .sessionAttr("user", member("owner", 1)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void missingAnswerMutationReturnsNotFound() throws Exception {
        when(answerService.update(any(Long.class), any(AnswerUpdateRequestDTO.class)))
                .thenThrow(new ResourceNotFoundException("Answer not found"));

        mockMvc.perform(put("/api/qna/404/answer")
                        .sessionAttr("user", member("admin", 2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정 답변\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void noticeAnswerAttemptReturnsBadRequest() throws Exception {
        when(answerService.save(any(AnswerSaveRequestDTO.class)))
                .thenThrow(new InvalidOperationException("A notice cannot have an answer"));

        mockMvc.perform(post("/api/qna/7/answer")
                        .sessionAttr("user", member("admin", 2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"답변\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void duplicateAnswerReturnsConflict() throws Exception {
        when(answerService.save(any(AnswerSaveRequestDTO.class)))
                .thenThrow(new ConflictException("Answer already exists"));

        mockMvc.perform(post("/api/qna/7/answer")
                        .sessionAttr("user", member("admin", 2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"답변\"}"))
                .andExpect(status().isConflict());
    }

    private MemberVO member(String id, int grade) {
        MemberVO member = new MemberVO();
        member.setId(id);
        member.setGrade(grade);
        return member;
    }

    private QuestionResponseDTO question(String id, int category) {
        QuestionResponseDTO question = mock(QuestionResponseDTO.class);
        when(question.getId()).thenReturn(id);
        when(question.getCategory()).thenReturn(category);
        return question;
    }
}
