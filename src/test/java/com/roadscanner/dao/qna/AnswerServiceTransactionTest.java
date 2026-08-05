package com.roadscanner.dao.qna;

import com.roadscanner.dto.AnswerSaveRequestDTO;
import com.roadscanner.service.qna.AnswerService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/dao-test-context.xml")
public class AnswerServiceTransactionTest {

    private static final long SAVE_QUESTION_NO = 9101L;
    private static final long DELETE_QUESTION_NO = 9102L;

    @Autowired
    private AnswerService answerService;

    @Autowired
    private DataSource dataSource;

    @Test
    public void answerInsertRollsBackWhenQuestionStatusUpdateFails() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        insertQuestion(jdbc, SAVE_QUESTION_NO, 30);
        jdbc.execute("ALTER TABLE QUESTION ADD CONSTRAINT CK_TX_ANSWER_SAVE "
                + "CHECK (no <> 9101 OR category <> 20)");

        try {
            AnswerSaveRequestDTO request = new AnswerSaveRequestDTO();
            request.setNo(SAVE_QUESTION_NO);
            request.setId("admin");
            request.setContent("transaction rollback test");

            assertThatThrownBy(() -> answerService.save(request))
                    .isInstanceOf(RuntimeException.class);

            assertThat(count(jdbc, "ANSWER", SAVE_QUESTION_NO)).isZero();
            assertThat(questionCategory(jdbc, SAVE_QUESTION_NO)).isEqualTo(30);
        } finally {
            jdbc.execute("ALTER TABLE QUESTION DROP CONSTRAINT CK_TX_ANSWER_SAVE");
            deleteFixture(jdbc, SAVE_QUESTION_NO);
        }
    }

    @Test
    public void answerDeleteRollsBackWhenQuestionStatusUpdateFails() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        insertQuestion(jdbc, DELETE_QUESTION_NO, 20);
        jdbc.update("INSERT INTO ANSWER (no, id, content, create_date) "
                + "VALUES (?, 'admin', 'transaction rollback test', CURRENT_TIMESTAMP)",
                DELETE_QUESTION_NO);
        jdbc.execute("ALTER TABLE QUESTION ADD CONSTRAINT CK_TX_ANSWER_DELETE "
                + "CHECK (no <> 9102 OR category <> 30)");

        try {
            assertThatThrownBy(() -> answerService.delete(DELETE_QUESTION_NO))
                    .isInstanceOf(RuntimeException.class);

            assertThat(count(jdbc, "ANSWER", DELETE_QUESTION_NO)).isEqualTo(1);
            assertThat(questionCategory(jdbc, DELETE_QUESTION_NO)).isEqualTo(20);
        } finally {
            jdbc.execute("ALTER TABLE QUESTION DROP CONSTRAINT CK_TX_ANSWER_DELETE");
            deleteFixture(jdbc, DELETE_QUESTION_NO);
        }
    }

    private void insertQuestion(JdbcTemplate jdbc, long no, int category) {
        jdbc.update("INSERT INTO QUESTION "
                        + "(no, category, views, idx, id, title, content, create_date, update_date) "
                        + "VALUES (?, ?, 0, NULL, 'admin', 'transaction test', "
                        + "'transaction test', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                no, category);
    }

    private int count(JdbcTemplate jdbc, String table, long no) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE no = ?", Integer.class, no);
        return count == null ? 0 : count;
    }

    private int questionCategory(JdbcTemplate jdbc, long no) {
        Integer category = jdbc.queryForObject(
                "SELECT category FROM QUESTION WHERE no = ?", Integer.class, no);
        return category == null ? 0 : category;
    }

    private void deleteFixture(JdbcTemplate jdbc, long no) {
        jdbc.update("DELETE FROM ANSWER WHERE no = ?", no);
        jdbc.update("DELETE FROM QUESTION WHERE no = ?", no);
    }
}
