package com.roadscanner.controller.upload;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
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
public class AnalysisApiFailureHandlingTest {

    @Mock
    private FileUploadService fileUploadService;

    @Mock
    private ResultImgService resultImgService;

    @Mock
    private RestTemplateService restTemplateService;

    private MockMvc mockMvc;

    @Before
    public void setUp() throws Exception {
        UploadController controller = new UploadController();
        controller.service = fileUploadService;
        controller.imgService = resultImgService;
        controller.restTemplateService = restTemplateService;
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new AnalysisApiExceptionHandler())
                .build();

        when(fileUploadService.doSelectOne(argThat(vo -> vo != null && vo.getIdx() == 42)))
                .thenReturn(storedUpload());
    }

    @Test
    public void analysisTimeoutMapsToGatewayTimeoutWithoutResponseDetails() throws Exception {
        when(restTemplateService.callFlaskApi("https://cdn.example.test/road.png"))
                .thenThrow(AnalysisApiException.timeout(new SocketTimeoutException("private detail")));

        mockMvc.perform(get("/main/upload")
                        .param("idx", "42")
                        .sessionAttr("user", member()))
                .andExpect(status().isGatewayTimeout())
                .andExpect(content().string(""));

        verify(resultImgService, never()).getResultImg(any(ResultImgVO.class));
    }

    @Test
    public void analysisConnectionFailureMapsToBadGatewayWithoutResponseDetails() throws Exception {
        when(restTemplateService.callFlaskApi("https://cdn.example.test/road.png"))
                .thenThrow(AnalysisApiException.connection(new ConnectException("private detail")));

        mockMvc.perform(get("/main/upload")
                        .param("idx", "42")
                        .sessionAttr("user", member()))
                .andExpect(status().isBadGateway())
                .andExpect(content().string(""));

        verify(resultImgService, never()).getResultImg(any(ResultImgVO.class));
    }

    @Test
    public void analysisServerFailureMapsToBadGateway() throws Exception {
        when(restTemplateService.callFlaskApi("https://cdn.example.test/road.png"))
                .thenThrow(AnalysisApiException.serverError(503, null));

        mockMvc.perform(get("/main/upload")
                        .param("idx", "42")
                        .sessionAttr("user", member()))
                .andExpect(status().isBadGateway());
    }

    private FileUploadVO storedUpload() {
        FileUploadVO upload = new FileUploadVO();
        upload.setIdx(42);
        upload.setId("owner");
        upload.setCategory(10);
        upload.setName("road.png");
        upload.setUrl("https://cdn.example.test/road.png");
        return upload;
    }

    private MemberVO member() {
        MemberVO member = new MemberVO();
        member.setId("owner");
        member.setGrade(1);
        return member;
    }
}
