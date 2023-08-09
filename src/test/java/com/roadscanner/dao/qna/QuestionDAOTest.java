package com.roadscanner.dao.qna;

import com.roadscanner.domain.qna.QuestionVO;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations =
        {
                "file:src/main/webapp/WEB-INF/root-context.xml",
                "file:src/main/resources/mybatis-config.xml"
        })
@TestPropertySource(locations = "classpath:application-prod.properties")
@Transactional
public class QuestionDAOTest {

    @Autowired
    private QuestionDAO dao;

    Long no;

    @Before
    public void setUp() {
        // 게시물 작성
        QuestionVO question = new QuestionVO(30, "jy", "테스트 시작2.","테스트 시작2.");
        no = dao.save(question);
    }

    @Test
    public void findByNo() {

        // given

        // when
        QuestionVO findByNoQuestion = dao.findByNo(no);

        // then
        assertThat(findByNoQuestion).isNotNull();
        assertThat(findByNoQuestion.getNo()).isEqualTo(no);

    }

    @Test
    public void findAll() {

        // given

        // when
        List<QuestionVO> questions = dao.findAll();

        // then
        assertThat(questions).isNotNull();
        assertThat(questions.size()).isGreaterThan(0);

    }

    @Test
    public void update() {

        // Given
        QuestionVO findByNoQuestion = dao.findByNo(no);
        findByNoQuestion.setTitle("제목 수정합니다.");

        // When
        dao.update(findByNoQuestion);

        // Then
        QuestionVO updateQuestion = dao.findByNo(no);
        assertThat(updateQuestion.getTitle()).isEqualTo("제목 수정합니다.");

    }

}