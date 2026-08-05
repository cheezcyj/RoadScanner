package com.roadscanner.controller.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.roadscanner.domain.result.ResultImgVO;
import com.roadscanner.domain.upload.FileUploadVO;
import com.roadscanner.domain.user.MemberVO;
import com.roadscanner.service.result.ResultImgService;
import com.roadscanner.service.upload.AnalysisApiException;
import com.roadscanner.service.upload.FileUploadService;
import com.roadscanner.service.upload.RestTemplateService;

@RunWith(MockitoJUnitRunner.class)
public class UploadControllerTest {

    @Mock
    private FileUploadService fileUploadService;

    @Mock
    private ResultImgService resultImgService;

    @Mock
    private RestTemplateService restTemplateService;

    private UploadController controller;

    @Before
    public void setUp() {
        controller = new UploadController();
        controller.service = fileUploadService;
        controller.imgService = resultImgService;
        controller.restTemplateService = restTemplateService;
    }

    @Test
    public void uploadUsesAuthenticatedOwnerAndInitialCategory() throws Exception {
        FileUploadVO request = new FileUploadVO();
        request.setId("spoofed");
        request.setCategory(30);
        MockMultipartFile file = new MockMultipartFile(
                "fileUpload", "road.png", "image/png", new byte[] { 1 });
        when(fileUploadService.doSave(file, request)).thenReturn("stored-road.png");
        FileUploadVO stored = storedUpload(42, "owner", 10, "stored-road.png",
                "https://cdn.example.test/stored-road.png");
        when(fileUploadService.doSelectOne(argThat(vo -> vo != null && "stored-road.png".equals(vo.getName()))))
                .thenReturn(stored);

        String response = controller.uploadFile(file, request, member("owner", 1));

        assertThat(response).isEqualTo("42");
        assertThat(request.getId()).isEqualTo("owner");
        assertThat(request.getCategory()).isEqualTo(10);
    }

    @Test
    public void resultUsesCanonicalRecordUrlForOwner() throws Exception {
        FileUploadVO stored = storedUpload(42, "owner", 10, "canonical-road.png",
                "https://cdn.example.test/canonical-road.png");
        ResultImgVO result = new ResultImgVO(7, "speed-limit", "limit", "https://cdn.example.test/result.png");
        when(fileUploadService.doSelectOne(argThat(vo -> vo != null && vo.getIdx() == 42))).thenReturn(stored);
        when(restTemplateService.callFlaskApi(stored.getUrl())).thenReturn(" 7\n");
        when(resultImgService.getResultImg(any(ResultImgVO.class))).thenReturn(result);
        Model model = new ExtendedModelMap();

        String view = controller.upload(42, model, new ResultImgVO(), member("owner", 1));

        assertThat(view).isEqualTo("upload");
        assertThat(model.asMap()).containsEntry("upload", stored).containsEntry("thisUrl", stored.getUrl())
                .containsEntry("resultImg", result);
        verify(restTemplateService).callFlaskApi("https://cdn.example.test/canonical-road.png");
    }

    @Test
    public void anotherMemberCannotAnalyzeStoredUpload() throws Exception {
        FileUploadVO stored = storedUpload(42, "owner", 10, "canonical-road.png",
                "https://cdn.example.test/canonical-road.png");
        when(fileUploadService.doSelectOne(argThat(vo -> vo != null && vo.getIdx() == 42))).thenReturn(stored);

        assertThatThrownBy(() -> controller.upload(42, new ExtendedModelMap(), new ResultImgVO(), member("other", 1)))
                .hasMessageContaining("403 FORBIDDEN");

        verify(restTemplateService, never()).callFlaskApi(any(String.class));
    }

    @Test
    public void administratorCanAnalyzeAnotherMembersStoredUpload() throws Exception {
        FileUploadVO stored = storedUpload(42, "owner", 10, "canonical-road.png",
                "https://cdn.example.test/canonical-road.png");
        ResultImgVO result = new ResultImgVO(1, "local-result", "local", "/local-result.png");
        when(fileUploadService.doSelectOne(argThat(vo -> vo != null && vo.getIdx() == 42))).thenReturn(stored);
        when(restTemplateService.callFlaskApi(stored.getUrl())).thenReturn("1");
        when(resultImgService.getResultImg(any(ResultImgVO.class))).thenReturn(result);
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.upload(42, model, new ResultImgVO(), member("localadmin", 2)))
                .isEqualTo("upload");
        assertThat(model.get("resultImg")).isSameAs(result);
        verify(restTemplateService).callFlaskApi(stored.getUrl());
    }

    @Test
    public void submittedFeedbackResultCanBeRefreshed() throws Exception {
        FileUploadVO stored = storedUpload(42, "owner", 30, "canonical-road.png",
                "https://cdn.example.test/canonical-road.png");
        ResultImgVO result = new ResultImgVO(7, "speed-limit", "limit", "https://cdn.example.test/result.png");
        when(fileUploadService.doSelectOne(argThat(vo -> vo != null && vo.getIdx() == 42))).thenReturn(stored);
        when(restTemplateService.callFlaskApi(stored.getUrl())).thenReturn("7");
        when(resultImgService.getResultImg(any(ResultImgVO.class))).thenReturn(result);

        String view = controller.upload(42, new ExtendedModelMap(), new ResultImgVO(), member("owner", 1));

        assertThat(view).isEqualTo("upload");
        verify(restTemplateService).callFlaskApi(stored.getUrl());
    }

    @Test
    public void missingResultMappingFailsClosedInsteadOfRenderingAnEmptyMatch() throws Exception {
        FileUploadVO stored = storedUpload(42, "owner", 10, "canonical-road.png",
                "https://cdn.example.test/canonical-road.png");
        when(fileUploadService.doSelectOne(argThat(vo -> vo != null && vo.getIdx() == 42)))
                .thenReturn(stored);
        when(restTemplateService.callFlaskApi(stored.getUrl())).thenReturn("44");
        when(resultImgService.getResultImg(any(ResultImgVO.class))).thenReturn(null);

        assertThatThrownBy(() -> controller.upload(
                42, new ExtendedModelMap(), new ResultImgVO(), member("owner", 1)))
                .isInstanceOf(AnalysisApiException.class);
    }

    @Test
    public void incompleteStoredUploadIsRejectedBeforeAnalysis() throws Exception {
        FileUploadVO missingUrl = storedUpload(42, "owner", 10, "canonical-road.png", null);
        when(fileUploadService.doSelectOne(argThat(vo -> vo != null && vo.getIdx() == 42)))
                .thenReturn(missingUrl);

        assertThatThrownBy(() -> controller.upload(
                42, new ExtendedModelMap(), new ResultImgVO(), member("owner", 1)))
                .hasMessageContaining("404 NOT_FOUND");

        verify(restTemplateService, never()).callFlaskApi(any(String.class));
    }

    @Test
    public void missingAndWrongCategoryUploadsCannotBeAnalyzed() throws Exception {
        when(fileUploadService.doSelectOne(argThat(vo -> vo != null && vo.getIdx() == 404))).thenReturn(null);
        FileUploadVO attachment = storedUpload(43, "owner", 40, "attachment.png",
                "https://cdn.example.test/attachment.png");
        when(fileUploadService.doSelectOne(argThat(vo -> vo != null && vo.getIdx() == 43))).thenReturn(attachment);

        assertThatThrownBy(() -> controller.upload(404, new ExtendedModelMap(), new ResultImgVO(), member("owner", 1)))
                .hasMessageContaining("404 NOT_FOUND");
        assertThatThrownBy(() -> controller.upload(43, new ExtendedModelMap(), new ResultImgVO(), member("owner", 1)))
                .hasMessageContaining("404 NOT_FOUND");

        verify(restTemplateService, never()).callFlaskApi(any(String.class));
    }

    @Test
    public void legacyMaliciousFilenameIsNotAcceptedAsResultLookup() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/main/upload")
                        .param("imgName", "\"><script>alert(1)</script>")
                        .sessionAttr("user", member("owner", 1)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/main/upload")
                        .param("idx", "<script>alert(1)</script>")
                        .sessionAttr("user", member("owner", 1)))
                .andExpect(status().isBadRequest());

        verify(fileUploadService, never()).doSelectOne(any(FileUploadVO.class));
    }

    @Test
    public void feedbackUsesStoredCanonicalRecordSelectedByIndex() throws Exception {
        FileUploadVO request = new FileUploadVO();
        request.setIdx(42);
        request.setName("attacker-controlled.png");
        request.setUrl("https://attacker.example/road.png");
        request.setCategory(20);
        FileUploadVO stored = storedUpload(42, "owner", 10, "canonical-road.png",
                "https://cdn.example.test/canonical-road.png");
        when(fileUploadService.doSelectOne(request)).thenReturn(stored);
        when(fileUploadService.doUpdate(stored)).thenReturn(1);

        String response = controller.feedbackUpdate(request, member("owner", 1));

        assertThat(response).contains("\"msgId\":\"1\"");
        assertThat(stored.getName()).isEqualTo("canonical-road.png");
        assertThat(stored.getUrl()).isEqualTo("https://cdn.example.test/canonical-road.png");
        assertThat(stored.getCategory()).isEqualTo(20);
        verify(fileUploadService).doUpdate(stored);
    }

    @Test
    public void feedbackRejectsUnexpectedCategoryWithoutMutatingStoredRecord() throws Exception {
        FileUploadVO request = new FileUploadVO();
        request.setIdx(42);
        request.setCategory(40);
        request.setU1(1);
        FileUploadVO stored = storedUpload(42, "owner", 10, "canonical-road.png",
                "https://cdn.example.test/canonical-road.png");
        when(fileUploadService.doSelectOne(request)).thenReturn(stored);

        assertThatThrownBy(() -> controller.feedbackUpdate(request, member("owner", 1)))
                .hasMessageContaining("400 BAD_REQUEST");

        assertThat(stored.getCategory()).isEqualTo(10);
        assertThat(stored.getU1()).isEqualTo(0);
        verify(fileUploadService, never()).doUpdate(any(FileUploadVO.class));
    }

    @Test
    public void positiveFeedbackRejectsReasonFlags() throws Exception {
        FileUploadVO request = new FileUploadVO();
        request.setIdx(42);
        request.setCategory(20);
        request.setU1(1);
        FileUploadVO stored = storedUpload(42, "owner", 10, "canonical-road.png",
                "https://cdn.example.test/canonical-road.png");
        when(fileUploadService.doSelectOne(request)).thenReturn(stored);

        assertThatThrownBy(() -> controller.feedbackUpdate(request, member("owner", 1)))
                .hasMessageContaining("400 BAD_REQUEST");

        assertThat(stored.getCategory()).isEqualTo(10);
        verify(fileUploadService, never()).doUpdate(any(FileUploadVO.class));
    }

    @Test
    public void negativeFeedbackRequiresAtLeastOneBinaryReason() throws Exception {
        FileUploadVO request = new FileUploadVO();
        request.setIdx(42);
        request.setCategory(30);
        request.setU1(2);
        FileUploadVO stored = storedUpload(42, "owner", 10, "canonical-road.png",
                "https://cdn.example.test/canonical-road.png");
        when(fileUploadService.doSelectOne(request)).thenReturn(stored);

        assertThatThrownBy(() -> controller.feedbackUpdate(request, member("owner", 1)))
                .hasMessageContaining("400 BAD_REQUEST");

        request.setU1(0);
        assertThatThrownBy(() -> controller.feedbackUpdate(request, member("owner", 1)))
                .hasMessageContaining("400 BAD_REQUEST");
        verify(fileUploadService, never()).doUpdate(any(FileUploadVO.class));
    }

    @Test
    public void failedSaveDoesNotAttemptToResolveAStoredRecord() throws Exception {
        FileUploadVO request = new FileUploadVO();
        MockMultipartFile file = new MockMultipartFile(
                "fileUpload", "road.png", "image/png", new byte[] { 1 });
        when(fileUploadService.doSave(file, request)).thenReturn("0");

        String response = controller.uploadFile(file, request, member("owner", 1));

        assertThat(response).startsWith("\"").endsWith("\"");
        verify(fileUploadService, never()).doSelectOne(any(FileUploadVO.class));
    }

    @Test
    public void anotherMemberCannotSubmitFeedbackForStoredFile() throws Exception {
        FileUploadVO request = new FileUploadVO();
        request.setIdx(42);
        request.setCategory(20);
        FileUploadVO stored = storedUpload(42, "owner", 10, "stored-road.png",
                "https://cdn.example.test/stored-road.png");
        when(fileUploadService.doSelectOne(request)).thenReturn(stored);

        assertThatThrownBy(() -> controller.feedbackUpdate(request, member("other", 1)))
                .hasMessageContaining("403 FORBIDDEN");

        verify(fileUploadService, never()).doUpdate(stored);
    }

    private FileUploadVO storedUpload(int idx, String id, int category, String name, String url) {
        FileUploadVO upload = new FileUploadVO();
        upload.setIdx(idx);
        upload.setId(id);
        upload.setCategory(category);
        upload.setName(name);
        upload.setUrl(url);
        return upload;
    }

    private MemberVO member(String id, int grade) {
        MemberVO member = new MemberVO();
        member.setId(id);
        member.setGrade(grade);
        return member;
    }
}
