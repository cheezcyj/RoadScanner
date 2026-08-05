package com.roadscanner.dao.qna;

import com.roadscanner.domain.qna.QuestionVO;
import com.roadscanner.dto.PaginationDTO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/dao-test-context.xml")
@Transactional
public class QuestionDAOTest {

    @Autowired
    private QuestionDAO dao;

    @Autowired
    private DataSource dataSource;

    private QuestionVO savedQuestion;

    @Before
    public void saveQuestion() {
        QuestionVO question = new QuestionVO(30, "admin", null,
                "DAO integration title", "DAO integration content");

        dao.save(question);

        savedQuestion = dao.findAll().stream()
                .filter(candidate -> question.getTitle().equals(candidate.getTitle()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("saved question was not found"));
    }

    @Test
    public void savesAndFindsQuestion() {
        QuestionVO found = dao.findByNo(savedQuestion.getNo());

        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo("admin");
        assertThat(found.getCategory()).isEqualTo(30);
        assertThat(found.getTitle()).isEqualTo("DAO integration title");
        assertThat(found.getContent()).isEqualTo("DAO integration content");
        assertThat(found.getCreateDate()).isNotNull();
    }

    @Test
    public void findsAllAndCountsQuestions() {
        List<QuestionVO> questions = dao.findAll();

        assertThat(questions).extracting(QuestionVO::getNo).contains(savedQuestion.getNo());
        int boardCount = (int) questions.stream()
                .filter(question -> question.getCategory() == 10 || question.getCategory() == 40)
                .count();
        assertThat(dao.countBoard(null, "both", null)).isEqualTo(boardCount);
    }

    @Test
    public void countsQuestionsThatReferenceAnAttachment() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("INSERT INTO UPLOAD_IMAGE "
                        + "(idx, id, category, upload_date, name, url, file_size, checked) "
                        + "VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?)",
                9001, "admin", 40, "shared-dao-test.png",
                "https://example.test/shared-dao-test.png", 1D, 0);
        savedQuestion.update(30, savedQuestion.getTitle(), 9001L, savedQuestion.getContent());
        dao.update(savedQuestion);

        assertThat(dao.countByAttachmentId(9001L)).isEqualTo(1);
        assertThat(dao.countByAttachmentId(9002L)).isZero();
    }

    @Test
    public void findsQuestionsWithPaging() {
        dao.save(new QuestionVO(40, "admin", null,
                "Public Q&A paging title", "Public Q&A paging content"));

        List<QuestionVO> page = dao.findBoardWithPaging(
                new PaginationDTO(1, 1), null, "both", null);

        assertThat(page).hasSize(1);
        assertThat(page).extracting(QuestionVO::getCategory).allMatch(
                category -> category == 10 || category == 40);
    }

    @Test
    public void findsAndCountsQuestionsByAuthor() {
        QuestionVO anotherAuthor = new QuestionVO(30, "member01", null,
                "Another author title", "Another author content");
        dao.save(anotherAuthor);

        List<QuestionVO> page = dao.findInquiriesByAuthorWithPaging(
                "admin", new PaginationDTO(1, 10), null, "both", null);

        assertThat(page).isNotEmpty();
        assertThat(page).extracting(QuestionVO::getId).containsOnly("admin");
        assertThat(dao.countInquiriesByAuthor("admin", null, "both", null)).isEqualTo(page.size());
        assertThat(dao.countInquiriesByAuthor("missing-user", null, "both", null)).isZero();
    }

    @Test
    public void appliesTheSameCategoryAndKeywordFiltersToRowsAndCount() {
        dao.save(new QuestionVO(20, "admin", null,
                "Answered road issue", "Resolved by maintenance"));
        dao.save(new QuestionVO(10, "admin", null,
                "Maintenance notice", "Road closure schedule"));

        PaginationDTO pagination = new PaginationDTO(1, 10);
        List<QuestionVO> answered = dao.findInquiriesWithPaging(
                pagination, 20, "title", "%road%");

        assertThat(answered).extracting(QuestionVO::getCategory).containsOnly(20);
        assertThat(answered).extracting(QuestionVO::getTitle)
                .allMatch(title -> title.toLowerCase().contains("road"));
        assertThat(dao.countInquiries(20, "title", "%road%"))
                .isEqualTo(answered.size());

        List<QuestionVO> notices = dao.findBoardWithPaging(
                pagination, 10, "content", "%closure%");
        assertThat(notices).hasSize(1);
        assertThat(notices).extracting(QuestionVO::getId).containsOnly("admin");
        assertThat(dao.countBoard(10, "content", "%closure%"))
                .isEqualTo(notices.size());
    }

    @Test
    public void treatsEscapedLikeWildcardsAsLiteralCharacters() {
        dao.save(new QuestionVO(30, "admin", null,
                "Coverage 100%_verified", "literal wildcard test"));

        List<QuestionVO> matches = dao.findInquiriesWithPaging(
                new PaginationDTO(1, 10), 30, "title", "%100\\%\\_verified%");

        assertThat(matches).extracting(QuestionVO::getTitle)
                .containsExactly("Coverage 100%_verified");
        assertThat(dao.countInquiries(30, "title", "%100\\%\\_verified%"))
                .isEqualTo(matches.size());
    }

    @Test
    public void boardAndInquiryQueriesCannotCrossVisibilityScopes() {
        QuestionVO boardInput = new QuestionVO(40, "member01", null,
                "Public Q&A only", "public-scope-token");
        QuestionVO privateInput = new QuestionVO(30, "member01", null,
                "Private inquiry only", "private-scope-token");
        dao.save(boardInput);
        dao.save(privateInput);

        QuestionVO board = dao.findAll().stream()
                .filter(question -> "Public Q&A only".equals(question.getTitle()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("board fixture was not found"));
        QuestionVO inquiry = dao.findAll().stream()
                .filter(question -> "Private inquiry only".equals(question.getTitle()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("inquiry fixture was not found"));

        List<QuestionVO> boardRows = dao.findBoardWithPaging(
                new PaginationDTO(1, 20), null, "both", null);
        List<QuestionVO> inquiryRows = dao.findInquiriesWithPaging(
                new PaginationDTO(1, 20), null, "both", null);

        assertThat(boardRows).extracting(QuestionVO::getNo)
                .contains(board.getNo())
                .doesNotContain(inquiry.getNo());
        assertThat(inquiryRows).extracting(QuestionVO::getNo)
                .contains(inquiry.getNo())
                .doesNotContain(board.getNo());
        assertThat(dao.countBoard(null, "content", "%private-scope-token%"))
                .isZero();
        assertThat(dao.countInquiries(null, "content", "%public-scope-token%"))
                .isZero();

        assertThat(dao.findBoardByNo(board.getNo())).isNotNull();
        assertThat(dao.findBoardByNo(inquiry.getNo())).isNull();
        assertThat(dao.findInquiryByNo(inquiry.getNo())).isNotNull();
        assertThat(dao.findInquiryByNo(board.getNo())).isNull();
        assertThat(dao.findInquiryByNoAndAuthor(inquiry.getNo(), "member01")).isNotNull();
        assertThat(dao.findInquiryByNoAndAuthor(inquiry.getNo(), "admin")).isNull();
    }

    @Test
    public void updatesQuestion() {
        savedQuestion.update(20, "updated title", null, "updated content");

        dao.update(savedQuestion);

        QuestionVO updated = dao.findByNo(savedQuestion.getNo());
        assertThat(updated.getCategory()).isEqualTo(20);
        assertThat(updated.getTitle()).isEqualTo("updated title");
        assertThat(updated.getContent()).isEqualTo("updated content");
        assertThat(updated.getUpdateDate()).isNotNull();
    }

    @Test
    public void increasesViews() {
        int before = savedQuestion.getViews();

        dao.increaseViews(savedQuestion.getNo());

        assertThat(dao.findByNo(savedQuestion.getNo()).getViews()).isEqualTo(before + 1);
    }

    @Test
    public void updatesCategory() {
        assertThat(dao.transitionCategory(savedQuestion.getNo(), 20, 10)).isZero();
        assertThat(dao.findByNo(savedQuestion.getNo()).getCategory()).isEqualTo(30);

        assertThat(dao.transitionCategory(savedQuestion.getNo(), 30, 20)).isEqualTo(1);

        assertThat(dao.findByNo(savedQuestion.getNo()).getCategory()).isEqualTo(20);
    }

    @Test
    public void deletesQuestion() {
        dao.delete(savedQuestion.getNo());

        assertThat(dao.findByNo(savedQuestion.getNo())).isNull();
    }
}
