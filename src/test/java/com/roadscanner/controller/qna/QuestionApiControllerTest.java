package com.roadscanner.controller.qna;

import com.roadscanner.domain.qna.QuestionVO;
import com.roadscanner.domain.user.MemberVO;
import com.roadscanner.dto.QuestionResponseDTO;
import com.roadscanner.dto.QuestionSaveRequestDTO;
import com.roadscanner.dto.QuestionUpdateRequestDTO;
import com.roadscanner.service.qna.QuestionService;
import com.roadscanner.service.upload.FileUploadService;
import com.roadscanner.domain.upload.FileUploadVO;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class QuestionApiControllerTest {

    @Mock
    private QuestionService questionService;

    @Mock
    private FileUploadService fileUploadService;

    @InjectMocks
    private QuestionApiController controller;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void boardSaveUsesSessionIdentityAndPreventsNoticeSpoofing() throws Exception {
        QuestionSaveRequestDTO request = new QuestionSaveRequestDTO();
        request.setId("spoofed-admin");
        request.setCategory(10);
        when(questionService.save(request)).thenReturn(1L);

        Long result = controller.save(request, member("member", 1));

        ArgumentCaptor<QuestionSaveRequestDTO> captor = ArgumentCaptor.forClass(QuestionSaveRequestDTO.class);
        verify(questionService).save(captor.capture());
        assertThat(result).isEqualTo(1L);
        assertThat(captor.getValue().getId()).isEqualTo("member");
        assertThat(captor.getValue().getCategory()).isEqualTo(40);
    }

    @Test
    public void inquirySaveUsesSessionIdentityAndForcesWaitingStatus() throws Exception {
        QuestionSaveRequestDTO request = new QuestionSaveRequestDTO();
        request.setId("spoofed-admin");
        request.setCategory(10);
        request.setTitle("문의");
        request.setContent("내용");
        when(questionService.save(request)).thenReturn(2L);

        Long result = controller.saveInquiry(request, member("member", 1));

        assertThat(result).isEqualTo(2L);
        assertThat(request.getId()).isEqualTo("member");
        assertThat(request.getCategory()).isEqualTo(30);
        verify(questionService).save(request);
    }

    @Test
    public void ownerCanUpdateButCannotChangeCategory() throws Exception {
        QuestionUpdateRequestDTO request = new QuestionUpdateRequestDTO(10, "수정", null, "내용");
        when(questionService.findByNo(5L)).thenReturn(question(5L, "owner", 20));
        when(questionService.update(5L, request)).thenReturn(5L);

        Long result = controller.update(5L, request, member("owner", 1));

        assertThat(result).isEqualTo(5L);
        assertThat(request.getCategory()).isEqualTo(20);
        verify(questionService).update(5L, request);
    }

    @Test
    public void anotherMemberCannotDeleteQuestion() {
        when(questionService.findByNo(5L)).thenReturn(question(5L, "owner", 30));

        assertThatThrownBy(() -> controller.delete(5L, member("other", 1)))
                .hasMessageContaining("403 FORBIDDEN");

        verify(questionService, never()).delete(5L);
    }

    @Test
    public void adminCanDeleteAnyQuestion() throws Exception {
        when(questionService.findByNo(5L)).thenReturn(question(5L, "owner", 30));
        when(questionService.delete(5L)).thenReturn(5L);

        assertThat(controller.delete(5L, member("admin", 2))).isEqualTo(5L);

        verify(questionService).delete(5L);
    }

    @Test
    public void invalidQuestionPayloadReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/qna/save")
                        .sessionAttr("user", member("member", 1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":30,\"title\":\" \",\"content\":\"<p><br></p>\"}"))
                .andExpect(status().isBadRequest());

        verify(questionService, never()).save(any(QuestionSaveRequestDTO.class));
    }

    @Test
    public void missingQuestionReturnsNotFound() throws Exception {
        when(questionService.findByNo(404L)).thenReturn(null);

        mockMvc.perform(get("/api/qna/404")
                        .sessionAttr("user", member("member", 1)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void jsonSaveRejectsEveryNonNullAttachmentId() throws Exception {
        QuestionSaveRequestDTO request = validSaveRequest(99L);

        assertThatThrownBy(() -> controller.save(request, member("owner", 1)))
                .hasMessageContaining("400 BAD_REQUEST");

        verify(questionService, never()).save(any(QuestionSaveRequestDTO.class));
        verifyNoInteractions(fileUploadService);
    }

    @Test
    public void jsonUpdateRejectsDifferentAttachmentIdWithoutLookup() throws Exception {
        QuestionUpdateRequestDTO request = new QuestionUpdateRequestDTO(30, "수정", 99L, "내용");
        when(questionService.findByNo(5L)).thenReturn(question(5L, "owner", 30, 10L));

        assertThatThrownBy(() -> controller.update(5L, request, member("owner", 1)))
                .hasMessageContaining("400 BAD_REQUEST");

        verify(questionService, never()).update(any(Long.class), any(QuestionUpdateRequestDTO.class));
        verifyNoInteractions(fileUploadService);
    }

    @Test
    public void jsonUpdateForcesExistingAttachmentToRemainWhenIdxIsOmitted() throws Exception {
        QuestionUpdateRequestDTO request = new QuestionUpdateRequestDTO(30, "수정", null, "내용");
        when(questionService.findByNo(5L)).thenReturn(question(5L, "owner", 30, 10L));
        when(questionService.update(5L, request)).thenReturn(5L);

        assertThat(controller.update(5L, request, member("owner", 1))).isEqualTo(5L);

        assertThat(request.getIdx()).isEqualTo(10L);
        verify(questionService).update(5L, request);
        verifyNoInteractions(fileUploadService);
    }

    @Test
    public void multipartSaveUploadsAttachmentBeforePersistingQuestion() throws Exception {
        QuestionSaveRequestDTO request = validSaveRequest(null);
        MockMultipartFile file = imageFile();
        FileUploadVO stored = attachment(71, "owner", 40);
        stored.setName("stored-road.png");

        when(fileUploadService.doSave(org.mockito.ArgumentMatchers.eq(file), any(FileUploadVO.class)))
                .thenReturn(stored.getName());
        when(fileUploadService.doSelectOne(any(FileUploadVO.class))).thenReturn(stored);
        when(questionService.save(any(QuestionSaveRequestDTO.class))).thenReturn(12L);

        Long result = controller.saveMultipart(request, file, member("owner", 1));

        assertThat(result).isEqualTo(12L);
        assertThat(request.getId()).isEqualTo("owner");
        assertThat(request.getCategory()).isEqualTo(40);
        assertThat(request.getIdx()).isEqualTo(71L);

        InOrder order = inOrder(fileUploadService, questionService);
        order.verify(fileUploadService).doSave(org.mockito.ArgumentMatchers.eq(file), any(FileUploadVO.class));
        order.verify(fileUploadService).doSelectOne(any(FileUploadVO.class));
        order.verify(questionService).save(request);
        verify(fileUploadService, never()).doDelete(any(FileUploadVO.class));
    }

    @Test
    public void multipartUpdateHttpContractBindsExplicitKeepAction() throws Exception {
        when(questionService.findByNo(5L)).thenReturn(question(5L, "owner", 30));
        when(questionService.update(org.mockito.ArgumentMatchers.eq(5L),
                any(QuestionUpdateRequestDTO.class))).thenReturn(5L);

        mockMvc.perform(multipart("/api/qna/5")
                        .param("category", "30")
                        .param("title", "updated")
                        .param("content", "content")
                        .param("attachmentAction", "KEEP")
                        .sessionAttr("user", member("owner", 1)))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string("5"));

        ArgumentCaptor<QuestionUpdateRequestDTO> captor =
                ArgumentCaptor.forClass(QuestionUpdateRequestDTO.class);
        verify(questionService).update(org.mockito.ArgumentMatchers.eq(5L), captor.capture());
        assertThat(captor.getValue().getIdx()).isNull();
    }

    @Test
    public void multipartSaveCompensatesNewAttachmentWhenQuestionPersistenceFails() throws Exception {
        QuestionSaveRequestDTO request = validSaveRequest(null);
        MockMultipartFile file = imageFile();
        FileUploadVO stored = attachment(72, "owner", 40);
        stored.setName("stored-road.png");

        when(fileUploadService.doSave(org.mockito.ArgumentMatchers.eq(file), any(FileUploadVO.class)))
                .thenReturn(stored.getName());
        when(fileUploadService.doSelectOne(any(FileUploadVO.class))).thenReturn(stored);
        when(questionService.save(any(QuestionSaveRequestDTO.class)))
                .thenThrow(new IllegalStateException("question database unavailable"));
        when(fileUploadService.doDelete(stored)).thenReturn(1);

        assertThatThrownBy(() -> controller.saveMultipart(request, file, member("owner", 1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("database unavailable");

        InOrder order = inOrder(fileUploadService, questionService);
        order.verify(fileUploadService).doSave(org.mockito.ArgumentMatchers.eq(file), any(FileUploadVO.class));
        order.verify(fileUploadService).doSelectOne(any(FileUploadVO.class));
        order.verify(questionService).save(request);
        order.verify(fileUploadService).doDelete(stored);
    }

    @Test
    public void multipartReplacePersistsQuestionBeforeDeletingOldAttachment() throws Exception {
        QuestionResponseDTO question = question(5L, "owner", 30, 10L);
        QuestionUpdateRequestDTO request = new QuestionUpdateRequestDTO(30, "updated", null, "content");
        MockMultipartFile file = imageFile();
        FileUploadVO oldAttachment = attachment(10, "owner", 40);
        oldAttachment.setName("old-road.png");
        FileUploadVO newAttachment = attachment(11, "owner", 40);
        newAttachment.setName("new-road.png");

        when(questionService.findByNo(5L)).thenReturn(question);
        when(fileUploadService.doSelectOne(any(FileUploadVO.class))).thenAnswer(invocation -> {
            FileUploadVO lookup = invocation.getArgument(0);
            return lookup.getIdx() == 10 ? oldAttachment : newAttachment;
        });
        when(fileUploadService.doSave(org.mockito.ArgumentMatchers.eq(file), any(FileUploadVO.class)))
                .thenReturn(newAttachment.getName());
        when(questionService.update(5L, request)).thenReturn(5L);
        when(fileUploadService.doDelete(oldAttachment)).thenReturn(1);

        Long result = controller.updateMultipart(5L, request,
                QuestionApiController.AttachmentAction.REPLACE, file, member("owner", 1));

        assertThat(result).isEqualTo(5L);
        assertThat(request.getIdx()).isEqualTo(11L);

        InOrder order = inOrder(questionService, fileUploadService);
        order.verify(questionService).findByNo(5L);
        order.verify(fileUploadService).doSelectOne(
                org.mockito.ArgumentMatchers.argThat(value -> value.getIdx() == 10));
        order.verify(fileUploadService).doSave(org.mockito.ArgumentMatchers.eq(file), any(FileUploadVO.class));
        order.verify(fileUploadService).doSelectOne(
                org.mockito.ArgumentMatchers.argThat(value -> "new-road.png".equals(value.getName())));
        order.verify(questionService).update(5L, request);
        order.verify(fileUploadService).doDelete(oldAttachment);
    }

    @Test
    public void multipartReplaceCompensatesOnlyNewAttachmentWhenQuestionUpdateFails() throws Exception {
        QuestionResponseDTO question = question(5L, "owner", 30, 10L);
        QuestionUpdateRequestDTO request = new QuestionUpdateRequestDTO(30, "updated", null, "content");
        MockMultipartFile file = imageFile();
        FileUploadVO oldAttachment = attachment(10, "owner", 40);
        oldAttachment.setName("old-road.png");
        FileUploadVO newAttachment = attachment(11, "owner", 40);
        newAttachment.setName("new-road.png");

        when(questionService.findByNo(5L)).thenReturn(question);
        when(fileUploadService.doSelectOne(any(FileUploadVO.class))).thenAnswer(invocation -> {
            FileUploadVO lookup = invocation.getArgument(0);
            return lookup.getIdx() == 10 ? oldAttachment : newAttachment;
        });
        when(fileUploadService.doSave(org.mockito.ArgumentMatchers.eq(file), any(FileUploadVO.class)))
                .thenReturn(newAttachment.getName());
        when(questionService.update(5L, request))
                .thenThrow(new IllegalStateException("question update failed"));
        when(fileUploadService.doDelete(newAttachment)).thenReturn(1);

        assertThatThrownBy(() -> controller.updateMultipart(5L, request,
                QuestionApiController.AttachmentAction.REPLACE, file, member("owner", 1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("update failed");

        InOrder order = inOrder(questionService, fileUploadService);
        order.verify(questionService).update(5L, request);
        order.verify(fileUploadService).doDelete(newAttachment);
        verify(fileUploadService, never()).doDelete(oldAttachment);
    }

    @Test
    public void multipartRemoveClearsReferenceBeforeDeletingOldAttachment() throws Exception {
        QuestionResponseDTO question = question(5L, "owner", 30, 10L);
        QuestionUpdateRequestDTO request = new QuestionUpdateRequestDTO(30, "updated", 999L, "content");
        FileUploadVO oldAttachment = attachment(10, "owner", 40);
        oldAttachment.setName("old-road.png");

        when(questionService.findByNo(5L)).thenReturn(question);
        when(fileUploadService.doSelectOne(any(FileUploadVO.class))).thenReturn(oldAttachment);
        when(questionService.update(5L, request)).thenReturn(5L);
        when(fileUploadService.doDelete(oldAttachment)).thenReturn(1);

        Long result = controller.updateMultipart(5L, request,
                QuestionApiController.AttachmentAction.REMOVE, null, member("owner", 1));

        assertThat(result).isEqualTo(5L);
        assertThat(request.getIdx()).isNull();
        InOrder order = inOrder(questionService, fileUploadService);
        order.verify(questionService).update(5L, request);
        order.verify(fileUploadService).doDelete(oldAttachment);
    }

    @Test
    public void sharedAttachmentIsNotDeletedWhileAnotherQuestionReferencesIt() throws Exception {
        QuestionResponseDTO question = question(5L, "owner", 30, 10L);
        QuestionUpdateRequestDTO request = new QuestionUpdateRequestDTO(30, "updated", null, "content");
        FileUploadVO sharedAttachment = attachment(10, "owner", 40);
        sharedAttachment.setName("shared-road.png");

        when(questionService.findByNo(5L)).thenReturn(question);
        when(fileUploadService.doSelectOne(any(FileUploadVO.class))).thenReturn(sharedAttachment);
        when(questionService.update(5L, request)).thenReturn(5L);
        when(questionService.countByAttachmentId(10L)).thenReturn(1);

        Long result = controller.updateMultipart(5L, request,
                QuestionApiController.AttachmentAction.REMOVE, null, member("owner", 1));

        assertThat(result).isEqualTo(5L);
        verify(questionService).countByAttachmentId(10L);
        verify(fileUploadService, never()).doDelete(sharedAttachment);
    }

    @Test
    public void ownerCanRemoveAdminOwnedAttachmentReferencedByTheQuestion() throws Exception {
        QuestionResponseDTO question = question(5L, "owner", 30, 10L);
        QuestionUpdateRequestDTO request = new QuestionUpdateRequestDTO(30, "updated", null, "content");
        FileUploadVO adminAttachment = attachment(10, "admin", 40);
        adminAttachment.setName("admin-replacement.png");

        when(questionService.findByNo(5L)).thenReturn(question);
        when(fileUploadService.doSelectOne(any(FileUploadVO.class))).thenReturn(adminAttachment);
        when(questionService.update(5L, request)).thenReturn(5L);
        when(fileUploadService.doDelete(adminAttachment)).thenReturn(1);

        Long result = controller.updateMultipart(5L, request,
                QuestionApiController.AttachmentAction.REMOVE, null, member("owner", 1));

        assertThat(result).isEqualTo(5L);
        verify(fileUploadService).doDelete(adminAttachment);
    }

    @Test
    public void deletePersistsQuestionRemovalBeforeDeletingAttachment() throws Exception {
        QuestionResponseDTO question = question(5L, "owner", 30, 10L);
        FileUploadVO oldAttachment = attachment(10, "owner", 40);
        oldAttachment.setName("old-road.png");

        when(questionService.findByNo(5L)).thenReturn(question);
        when(fileUploadService.doSelectOne(any(FileUploadVO.class))).thenReturn(oldAttachment);
        when(questionService.delete(5L)).thenReturn(5L);
        when(fileUploadService.doDelete(oldAttachment)).thenReturn(1);

        assertThat(controller.delete(5L, member("owner", 1))).isEqualTo(5L);

        InOrder order = inOrder(questionService, fileUploadService);
        order.verify(questionService).delete(5L);
        order.verify(fileUploadService).doDelete(oldAttachment);
    }

    @Test
    public void boardQuestionCanBeReadByAnyAuthenticatedMember() throws Exception {
        when(questionService.findByNo(7L)).thenReturn(question(7L, "owner", 40));

        mockMvc.perform(get("/api/qna/7")
                        .sessionAttr("user", member("other", 1)))
                .andExpect(status().isOk());
    }

    @Test
    public void privateInquiryCannotBeReadByAnotherMemberButOwnerAndAdminCanReadIt() throws Exception {
        when(questionService.findByNo(8L)).thenReturn(question(8L, "owner", 30));

        mockMvc.perform(get("/api/qna/8")
                        .sessionAttr("user", member("other", 1)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/qna/8")
                        .sessionAttr("user", member("owner", 1)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/qna/8")
                        .sessionAttr("user", member("admin", 2)))
                .andExpect(status().isOk());
    }

    @Test
    public void administratorCannotMoveQuestionBetweenBoardAndInquiryScopesOnUpdate() throws Exception {
        QuestionUpdateRequestDTO request = new QuestionUpdateRequestDTO(30, "수정", null, "내용");
        when(questionService.findByNo(12L)).thenReturn(question(12L, "owner", 40));
        when(questionService.update(12L, request)).thenReturn(12L);

        assertThat(controller.update(12L, request, member("admin", 2))).isEqualTo(12L);

        assertThat(request.getCategory()).isEqualTo(40);
        verify(questionService).update(12L, request);
    }

    private QuestionSaveRequestDTO validSaveRequest(Long idx) {
        QuestionSaveRequestDTO request = new QuestionSaveRequestDTO();
        request.setCategory(30);
        request.setIdx(idx);
        request.setTitle("제목");
        request.setContent("내용");
        return request;
    }

    private FileUploadVO attachment(int idx, String id, int category) {
        FileUploadVO attachment = new FileUploadVO();
        attachment.setIdx(idx);
        attachment.setId(id);
        attachment.setCategory(category);
        return attachment;
    }

    private MockMultipartFile imageFile() {
        return new MockMultipartFile("fileUpload", "road.png", "image/png", new byte[] { 1 });
    }

    private QuestionResponseDTO question(Long no, String id, int category) {
        return question(no, id, category, null);
    }

    private QuestionResponseDTO question(Long no, String id, int category, Long attachmentId) {
        QuestionVO question = new QuestionVO();
        question.setNo(no);
        question.setId(id);
        question.setCategory(category);
        question.setIdx(attachmentId);
        question.setTitle("제목");
        question.setContent("내용");
        question.setViews(0);
        question.setCreateDate(LocalDateTime.of(2026, 8, 1, 10, 0));
        question.setUpdateDate(question.getCreateDate());
        return new QuestionResponseDTO(question);
    }

    private MemberVO member(String id, int grade) {
        MemberVO member = new MemberVO();
        member.setId(id);
        member.setGrade(grade);
        return member;
    }
}
