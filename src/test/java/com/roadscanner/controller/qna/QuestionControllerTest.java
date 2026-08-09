package com.roadscanner.controller.qna;

import com.roadscanner.domain.user.MemberVO;
import com.roadscanner.dto.QuestionResponseDTO;
import com.roadscanner.service.qna.AnswerService;
import com.roadscanner.service.qna.QuestionService;
import com.roadscanner.service.upload.FileUploadService;
import com.roadscanner.service.user.UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@RunWith(MockitoJUnitRunner.class)
public class QuestionControllerTest {

    @Mock
    private QuestionService questionService;

    @Mock
    private AnswerService answerService;

    @Mock
    private FileUploadService fileUploadService;

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        QuestionController controller = new QuestionController(
                questionService, answerService, fileUploadService, userService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void separateQuestionAttachmentUploadIsGone() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "fileUpload", "road.png", "image/png", new byte[] { 1 });

        mockMvc.perform(multipart("/qna/fileUploaded")
                        .file(file)
                        .sessionAttr("user", member("owner", 1)))
                .andExpect(status().isGone());

        verifyNoInteractions(fileUploadService);
    }

    @Test
    public void directQuestionAttachmentDeleteIsGone() throws Exception {
        mockMvc.perform(delete("/qna/fileDelete")
                        .param("name", "legacy-name.png")
                        .sessionAttr("user", member("owner", 1)))
                .andExpect(status().isGone());

        verifyNoInteractions(fileUploadService);
        verifyNoInteractions(questionService);
    }

    @Test
    public void myQuestionsUsesAuthenticatedAuthorAndValidEmptyPage() throws Exception {
        when(questionService.countInquiriesByAuthor("owner", null, "both", null)).thenReturn(0);
        when(questionService.findInquiriesByAuthorWithPaging(
                eq("owner"), any(), isNull(), eq("both"), isNull()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/qna/my")
                        .param("page", "99")
                        .sessionAttr("user", member("owner", 1)))
                .andExpect(status().isOk())
                .andExpect(view().name("qna/index"))
                .andExpect(model().attribute("mineOnly", true))
                .andExpect(model().attribute("viewMode", "myInquiry"))
                .andExpect(model().attribute("listPath", "/qna/my"))
                .andExpect(model().attribute("page", 1))
                .andExpect(model().attribute("totalPages", 1));

        verify(questionService).countInquiriesByAuthor("owner", null, "both", null);
        verify(questionService).findInquiriesByAuthorWithPaging(
                eq("owner"), any(), isNull(), eq("both"), isNull());
    }

    @Test
    public void questionIndexClampsOutOfRangePageBeforeQuery() throws Exception {
        when(questionService.countBoard(null, "both", null)).thenReturn(11);
        when(questionService.findBoardWithPaging(
                any(), isNull(), eq("both"), isNull()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/qna").param("page", "99").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("qna/index"))
                .andExpect(model().attribute("page", 2))
                .andExpect(model().attribute("totalPages", 2))
                .andExpect(model().attribute("size", 10))
                .andExpect(model().attribute("category", ""))
                .andExpect(model().attribute("searchType", "both"));
    }

    @Test
    public void questionIndexMapsEveryCategoryAndPreservesSafeFilters() throws Exception {
        String[][] categories = {
                { "notice", "10" },
                { "general", "40" }
        };

        for (String[] category : categories) {
            int categoryCode = Integer.parseInt(category[1]);
            when(questionService.countBoard(categoryCode, "title", "road"))
                    .thenReturn(0);
            when(questionService.findBoardWithPaging(
                    any(), eq(categoryCode), eq("title"), eq("road")))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/qna")
                            .param("category", category[0])
                            .param("searchType", "title")
                            .param("keyword", "  road  "))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("category", category[0]))
                    .andExpect(model().attribute("searchType", "title"))
                    .andExpect(model().attribute("keyword", "road"));

            verify(questionService).countBoard(categoryCode, "title", "road");
        }
    }

    @Test
    public void myQuestionsRejectsUnknownFiltersAndLimitsKeywordLength() throws Exception {
        String longKeyword = String.join("", Collections.nCopies(101, "a"));
        String limitedKeyword = String.join("", Collections.nCopies(100, "a"));
        when(questionService.countInquiriesByAuthor("owner", null, "both", limitedKeyword))
                .thenReturn(0);
        when(questionService.findInquiriesByAuthorWithPaging(
                eq("owner"), any(), isNull(), eq("both"), eq(limitedKeyword)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/qna/my")
                        .sessionAttr("user", member("owner", 1))
                        .param("category", "invalid")
                        .param("searchType", "id")
                        .param("keyword", " " + longKeyword + " "))
                .andExpect(status().isOk())
                .andExpect(model().attribute("category", ""))
                .andExpect(model().attribute("searchType", "both"))
                .andExpect(model().attribute("keyword", limitedKeyword));

        verify(questionService).countInquiriesByAuthor("owner", null, "both", limitedKeyword);
    }

    @Test
    public void detailDisplaysTheViewCountIncrementedForCurrentRequest() throws Exception {
        QuestionResponseDTO question = mock(QuestionResponseDTO.class);
        when(question.getViews()).thenReturn(4);
        when(question.getIdx()).thenReturn(null);
        when(question.getCategory()).thenReturn(40);
        when(questionService.findByNo(7L)).thenReturn(question);

        mockMvc.perform(get("/qna/7"))
                .andExpect(status().isOk())
                .andExpect(view().name("qna/question-detail"))
                .andExpect(model().attribute("question", question));

        verify(questionService).increaseViews(7L);
        verify(question).setViews(5);
    }

    @Test
    public void anotherMemberCannotOpenPrivateInquiry() throws Exception {
        QuestionResponseDTO inquiry = response(9L, "owner", 30);
        when(questionService.findByNo(9L)).thenReturn(inquiry);
        when(userService.selectUser(any(MemberVO.class))).thenReturn(member("other", 1));

        mockMvc.perform(get("/qna/9").sessionAttr("user", member("other", 1)))
                .andExpect(status().isForbidden());

        verify(questionService, org.mockito.Mockito.never()).increaseViews(9L);
        verifyNoInteractions(answerService);
        verifyNoInteractions(fileUploadService);
    }

    @Test
    public void ownerCanOpenPrivateInquiryAndUsesOwnInquiryReturnPath() throws Exception {
        QuestionResponseDTO inquiry = response(9L, "owner", 30);
        when(questionService.findByNo(9L)).thenReturn(inquiry);
        when(userService.selectUser(any(MemberVO.class))).thenReturn(member("owner", 1));

        mockMvc.perform(get("/qna/9").sessionAttr("user", member("owner", 1)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("inquiryMode", true))
                .andExpect(model().attribute("listPath", "/qna/my"));

        verify(questionService).increaseViews(9L);
        verify(answerService).findByNo(9L);
    }

    @Test
    public void staleAdministratorSessionCannotOpenPrivateInquiry() throws Exception {
        QuestionResponseDTO inquiry = response(9L, "owner", 30);
        when(questionService.findByNo(9L)).thenReturn(inquiry);
        MemberVO demoted = member("former-admin", 1);
        demoted.setCredentialVersion(2);
        when(userService.selectUser(any(MemberVO.class))).thenReturn(demoted);

        mockMvc.perform(get("/qna/9")
                        .sessionAttr("user", member("former-admin", 2)))
                .andExpect(status().isForbidden());

        verify(questionService, org.mockito.Mockito.never()).increaseViews(9L);
    }

    @Test
    public void inquiryManagementRequiresAdministratorAndUsesInquiryScope() throws Exception {
        mockMvc.perform(get("/qna/inquiries")
                        .sessionAttr("user", member("owner", 1)))
                .andExpect(status().isForbidden());

        when(questionService.countInquiries(null, "both", null)).thenReturn(0);
        when(questionService.findInquiriesWithPaging(
                any(), isNull(), eq("both"), isNull())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/qna/inquiries")
                        .sessionAttr("user", member("admin", 2)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("adminInquiryMode", true))
                .andExpect(model().attribute("listPath", "/qna/inquiries"));
    }

    @Test
    public void inquiryWritePageIsSeparatedFromBoardWritePage() throws Exception {
        mockMvc.perform(get("/qna/inquiry/save")
                        .sessionAttr("user", member("owner", 1)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("inquiryMode", true))
                .andExpect(model().attribute("viewMode", "myInquiry"))
                .andExpect(model().attribute("listPath", "/qna/my"))
                .andExpect(model().attribute("returnPath", "/qna/my"));

        mockMvc.perform(get("/qna/save")
                        .sessionAttr("user", member("owner", 1)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("inquiryMode", false))
                .andExpect(model().attribute("viewMode", "board"))
                .andExpect(model().attribute("listPath", "/qna"))
                .andExpect(model().attribute("returnPath", "/qna"));
    }

    private QuestionResponseDTO response(Long no, String id, int category) {
        QuestionResponseDTO response = mock(QuestionResponseDTO.class);
        org.mockito.Mockito.lenient().when(response.getNo()).thenReturn(no);
        org.mockito.Mockito.lenient().when(response.getId()).thenReturn(id);
        org.mockito.Mockito.lenient().when(response.getCategory()).thenReturn(category);
        org.mockito.Mockito.lenient().when(response.getIdx()).thenReturn(null);
        org.mockito.Mockito.lenient().when(response.getViews()).thenReturn(0);
        return response;
    }

    private MemberVO member(String id, int grade) {
        MemberVO member = new MemberVO();
        member.setId(id);
        member.setGrade(grade);
        return member;
    }
}
