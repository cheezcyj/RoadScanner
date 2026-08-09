package com.roadscanner.dao.qna;

import com.roadscanner.domain.qna.AnswerVO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/dao-test-context.xml")
@Transactional
public class AnswerDAOTest {

    private static final long QUESTION_NO = 229L;

    @Autowired
    private AnswerDAO dao;

    private AnswerVO answer;

    @Before
    public void saveAnswer() {
        answer = new AnswerVO(QUESTION_NO, "admin", "answer DAO integration content");
        dao.save(answer);
    }

    @Test
    public void savesAndFindsAnswer() {
        AnswerVO found = dao.findByNo(QUESTION_NO);

        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo("admin");
        assertThat(found.getContent()).isEqualTo(answer.getContent());
        assertThat(found.getCreateDate()).isNotNull();
        assertThat(found.getUpdateDate()).isNull();
    }

    @Test
    public void updatesAnswer() {
        answer.update("updated answer content");

        dao.update(answer);

        AnswerVO updated = dao.findByNo(QUESTION_NO);
        assertThat(updated.getContent()).isEqualTo("updated answer content");
        assertThat(updated.getUpdateDate()).isNotNull();
    }

    @Test
    public void deletesAnswer() {
        dao.delete(QUESTION_NO);

        assertThat(dao.findByNo(QUESTION_NO)).isNull();
    }
}
