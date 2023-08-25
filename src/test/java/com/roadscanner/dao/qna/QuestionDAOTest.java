package com.roadscanner.dao.qna;

import com.roadscanner.domain.qna.QuestionVO;
import org.assertj.core.api.Assertions;
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

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations =
        {
                "file:src/main/webapp/WEB-INF/root-context.xml",
                "file:src/main/resources/mybatis-config.xml"
        })
@Transactional
public class QuestionDAOTest {

    @Autowired
    private QuestionDAO dao;

    /**
     * 각 메서드 마다 실행 전 게시물 작성
     */
    @Before
    public void save() {

        QuestionVO question = new QuestionVO();
        question.setNo(1L);
        question.setCategory(30);
        question.setId("admin");
        question.setTitle("테스트 제목1");
        question.setContent("테스트 내용1");

        dao.save(question);

    }

    /**
     * 단건 조회
     */
    @Test
    public void findByNo() {
        Long no = 1L; // 시퀀스 포기
        QuestionVO question = dao.findByNo(no);
        assertThat(question).isNotNull();
        assertThat(question.getNo()).isEqualTo(no);
    }

    @Test
    public void findAll() {
        List<QuestionVO> questions = dao.findAll();
        assertThat(questions).isNotNull();
        assertThat(questions.size()).isGreaterThan(0);
    }

    @Test
    public void update() {
        Long no = 1L; // 예시로 1번을 업데이트
        QuestionVO question = dao.findByNo(no);
        question.setTitle("Updated title");
        question.setContent("Updated content");

        dao.update(question);
        QuestionVO updatedQuestion = dao.findByNo(no);
        assertThat(updatedQuestion.getTitle()).isEqualTo("Updated title");
        assertThat(updatedQuestion.getContent()).isEqualTo("Updated content");
    }

    @Test
    public void delete() {
        Long no = 1L; // 예시로 1번을 삭제
        dao.delete(no);
        QuestionVO deletedQuestion = dao.findByNo(no);
        assertThat(deletedQuestion).isNull();
    }


}