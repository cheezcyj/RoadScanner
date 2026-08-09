package com.roadscanner.service.qna;

import com.roadscanner.dao.qna.QuestionDAO;
import com.roadscanner.domain.qna.QuestionVO;
import com.roadscanner.dto.PaginationDTO;
import com.roadscanner.dto.QuestionListResponseDTO;
import com.roadscanner.dto.QuestionResponseDTO;
import com.roadscanner.dto.QuestionSaveRequestDTO;
import com.roadscanner.dto.QuestionUpdateRequestDTO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QuestionServiceImplTest {

    @Mock
    private QuestionDAO questionDAO;

    @InjectMocks
    private QuestionServiceImpl questionService;

    @Test
    public void findAllWithPagingMapsQuestions() {
        PaginationDTO pagination = new PaginationDTO(2, 10);
        QuestionVO question = question(7L, "제목", "내용", 11L);
        when(questionDAO.findBoardWithPaging(pagination, 40, "title", "%road\\_100\\%%"))
                .thenReturn(Collections.singletonList(question));

        List<QuestionListResponseDTO> result = questionService.findBoardWithPaging(
                pagination, 40, "title", " road_100% ");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNo()).isEqualTo(7L);
        assertThat(result.get(0).getTitle()).isEqualTo("제목");
        verify(questionDAO).findBoardWithPaging(pagination, 40, "title", "%road\\_100\\%%");
    }

    @Test
    public void findByAuthorWithPagingMapsOnlyRequestedAuthor() {
        PaginationDTO pagination = new PaginationDTO(1, 10);
        QuestionVO question = question(8L, "내 문의", "문의 내용", null);
        when(questionDAO.findInquiriesByAuthorWithPaging(
                "member", pagination, 30, "content", "%question%"))
                .thenReturn(Collections.singletonList(question));
        when(questionDAO.countInquiriesByAuthor("member", 30, "content", "%question%"))
                .thenReturn(1);

        List<QuestionListResponseDTO> result =
                questionService.findInquiriesByAuthorWithPaging(
                        "member", pagination, 30, "content", "question");

        assertThat(result).extracting(QuestionListResponseDTO::getId).containsOnly("member");
        assertThat(questionService.countInquiriesByAuthor(
                "member", 30, "content", "question")).isEqualTo(1);
        verify(questionDAO).findInquiriesByAuthorWithPaging(
                "member", pagination, 30, "content", "%question%");
        verify(questionDAO).countInquiriesByAuthor("member", 30, "content", "%question%");
    }

    @Test
    public void adminInquiryQueriesRemainInsideInquiryCategories() {
        PaginationDTO pagination = new PaginationDTO(1, 10);
        QuestionVO inquiry = question(9L, "Inquiry", "Private content", null);
        when(questionDAO.findInquiriesWithPaging(pagination, 20, "both", null))
                .thenReturn(Collections.singletonList(inquiry));
        when(questionDAO.countInquiries(20, "both", null)).thenReturn(1);

        assertThat(questionService.findInquiriesWithPaging(
                pagination, 20, "both", null)).hasSize(1);
        assertThat(questionService.countInquiries(20, "both", null)).isEqualTo(1);

        verify(questionDAO).findInquiriesWithPaging(pagination, 20, "both", null);
        verify(questionDAO).countInquiries(20, "both", null);
    }

    @Test
    public void crossScopeCategoryFiltersCannotBroadenDaoQueries() {
        PaginationDTO pagination = new PaginationDTO(1, 10);
        when(questionDAO.findBoardWithPaging(pagination, null, "both", null))
                .thenReturn(Collections.emptyList());
        when(questionDAO.findInquiriesWithPaging(pagination, null, "both", null))
                .thenReturn(Collections.emptyList());

        questionService.findBoardWithPaging(pagination, 30, "both", null);
        questionService.findInquiriesWithPaging(pagination, 40, "both", null);

        verify(questionDAO).findBoardWithPaging(pagination, null, "both", null);
        verify(questionDAO).findInquiriesWithPaging(pagination, null, "both", null);
    }

    @Test
    public void saveConvertsDtoIncludingAttachmentId() {
        QuestionSaveRequestDTO request = new QuestionSaveRequestDTO();
        request.setCategory(30);
        request.setId("member");
        request.setIdx(42L);
        request.setTitle("제목");
        request.setContent("내용");
        when(questionDAO.save(any(QuestionVO.class))).thenReturn(1L);

        Long result = questionService.save(request);

        ArgumentCaptor<QuestionVO> captor = ArgumentCaptor.forClass(QuestionVO.class);
        verify(questionDAO).save(captor.capture());
        QuestionVO saved = captor.getValue();
        assertThat(result).isEqualTo(1L);
        assertThat(saved.getCategory()).isEqualTo(30);
        assertThat(saved.getId()).isEqualTo("member");
        assertThat(saved.getIdx()).isEqualTo(42L);
        assertThat(saved.getTitle()).isEqualTo("제목");
        assertThat(saved.getContent()).isEqualTo("내용");
    }

    @Test
    public void saveSanitizesRichTextBeforePersistence() {
        QuestionSaveRequestDTO request = new QuestionSaveRequestDTO();
        request.setCategory(30);
        request.setId("member");
        request.setTitle("Road report");
        request.setContent("<h2>Summary</h2><p onclick=\"alert(1)\"><strong>Keep</strong> this"
                + "<script>alert(2)</script></p><img src=x onerror=\"alert(3)\">");
        when(questionDAO.save(any(QuestionVO.class))).thenReturn(11L);

        assertThat(questionService.save(request)).isEqualTo(11L);

        ArgumentCaptor<QuestionVO> captor = ArgumentCaptor.forClass(QuestionVO.class);
        verify(questionDAO).save(captor.capture());
        assertThat(captor.getValue().getContent())
                .contains("<h2>Summary</h2>", "<p><strong>Keep</strong> this</p>")
                .doesNotContain("onclick", "<script", "alert(2)", "<img", "onerror");
    }

    @Test
    public void saveRejectsAnsweredStatusAsAnInitialCategory() {
        QuestionSaveRequestDTO request = new QuestionSaveRequestDTO();
        request.setCategory(20);
        request.setId("member");
        request.setTitle("Invalid initial state");
        request.setContent("Content");

        assertThatThrownBy(() -> questionService.save(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("category");

        verify(questionDAO, never()).save(any(QuestionVO.class));
    }

    @Test
    public void findByNoMapsQuestion() {
        QuestionVO question = question(3L, "상세 제목", "상세 내용", 9L);
        when(questionDAO.findByNo(3L)).thenReturn(question);

        QuestionResponseDTO result = questionService.findByNo(3L);

        assertThat(result.getNo()).isEqualTo(3L);
        assertThat(result.getIdx()).isEqualTo(9L);
        assertThat(result.getTitle()).isEqualTo("상세 제목");
        assertThat(result.getContent()).isEqualTo("상세 내용");
    }

    @Test
    public void findByNoSanitizesLegacyRichTextWhenMappingResponse() {
        QuestionVO question = question(13L, "Legacy",
                "<blockquote cite=\"unsafe\"><em>Allowed</em>"
                        + "<iframe src=\"https://example.invalid\"></iframe>"
                        + "<svg onload=\"alert(1)\"></svg></blockquote>", null);
        when(questionDAO.findByNo(13L)).thenReturn(question);

        QuestionResponseDTO result = questionService.findByNo(13L);

        assertThat(result.getContent())
                .contains("<blockquote><em>Allowed</em></blockquote>")
                .doesNotContain("cite", "<iframe", "example.invalid", "<svg", "onload");
    }

    @Test
    public void scopedDetailMethodsDelegateToScopedDaoQueries() {
        QuestionVO board = question(14L, "Public Q&A", "Visible", null);
        board.setCategory(40);
        QuestionVO inquiry = question(15L, "Inquiry", "Private", null);
        when(questionDAO.findBoardByNo(14L)).thenReturn(board);
        when(questionDAO.findInquiryByNo(15L)).thenReturn(inquiry);
        when(questionDAO.findInquiryByNoAndAuthor(15L, "member")).thenReturn(inquiry);

        assertThat(questionService.findBoardByNo(14L).getCategory()).isEqualTo(40);
        assertThat(questionService.findInquiryByNo(15L).getCategory()).isEqualTo(30);
        assertThat(questionService.findInquiryByNoAndAuthor(15L, "member").getId())
                .isEqualTo("member");
    }

    @Test
    public void findByNoReturnsNullWhenQuestionDoesNotExist() {
        when(questionDAO.findByNo(404L)).thenReturn(null);

        assertThat(questionService.findByNo(404L)).isNull();
    }

    @Test
    public void updatePreservesPersistedQuestionCategory() {
        QuestionVO question = question(5L, "기존 제목", "기존 내용", null);
        QuestionUpdateRequestDTO request = new QuestionUpdateRequestDTO(20, "새 제목", 15L, "새 내용");
        when(questionDAO.findByNo(5L)).thenReturn(question);
        when(questionDAO.update(any(QuestionVO.class))).thenReturn(1);

        Long result = questionService.update(5L, request);

        assertThat(result).isEqualTo(5L);
        assertThat(question.getCategory()).isEqualTo(30);
        assertThat(question.getTitle()).isEqualTo("새 제목");
        assertThat(question.getIdx()).isEqualTo(15L);
        assertThat(question.getContent()).isEqualTo("새 내용");
        verify(questionDAO).update(question);
    }

    @Test
    public void updateSanitizesRichTextBeforePersistence() {
        QuestionVO question = question(6L, "Before", "<p>Before</p>", null);
        QuestionUpdateRequestDTO request = new QuestionUpdateRequestDTO(
                20,
                "After",
                16L,
                "<div class=\"unsafe\"><u>Updated</u>"
                        + "<a href=\"javascript:alert(1)\">link</a>"
                        + "<ol><li>Kept item</li></ol></div>");
        when(questionDAO.findByNo(6L)).thenReturn(question);
        when(questionDAO.update(any(QuestionVO.class))).thenReturn(1);

        assertThat(questionService.update(6L, request)).isEqualTo(6L);

        assertThat(question.getContent())
                .contains("<div><u>Updated</u>link<ol><li>Kept item</li></ol></div>")
                .doesNotContain("class=", "<a", "href", "javascript:");
        verify(questionDAO).update(question);
    }

    @Test
    public void delegatesDeleteCountAndViewIncrease() {
        when(questionDAO.countBoard(null, "both", null)).thenReturn(12);
        when(questionDAO.countByAttachmentId(42L)).thenReturn(2);
        when(questionDAO.delete(8L)).thenReturn(1);

        assertThat(questionService.delete(8L)).isEqualTo(8L);
        assertThat(questionService.countBoard(99, "unsupported", "  ")).isEqualTo(12);
        assertThat(questionService.countByAttachmentId(42L)).isEqualTo(2);
        questionService.increaseViews(8L);

        verify(questionDAO).delete(8L);
        verify(questionDAO).countBoard(null, "both", null);
        verify(questionDAO).countByAttachmentId(42L);
        verify(questionDAO).increaseViews(8L);
    }

    @Test
    public void updateAndDeleteRejectZeroAffectedRows() {
        QuestionVO question = question(5L, "기존 제목", "기존 내용", null);
        QuestionUpdateRequestDTO request = new QuestionUpdateRequestDTO(20, "새 제목", null, "새 내용");
        when(questionDAO.findByNo(5L)).thenReturn(question);
        when(questionDAO.update(any(QuestionVO.class))).thenReturn(0);
        when(questionDAO.delete(8L)).thenReturn(0);

        assertThatThrownBy(() -> questionService.update(5L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exactly one row");
        assertThatThrownBy(() -> questionService.delete(8L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exactly one row");
    }

    private QuestionVO question(Long no, String title, String content, Long idx) {
        QuestionVO question = new QuestionVO();
        question.setNo(no);
        question.setCategory(30);
        question.setViews(4);
        question.setIdx(idx);
        question.setId("member");
        question.setTitle(title);
        question.setContent(content);
        question.setCreateDate(LocalDateTime.of(2026, 7, 31, 10, 20, 30));
        question.setUpdateDate(LocalDateTime.of(2026, 7, 31, 10, 20, 30));
        return question;
    }
}
