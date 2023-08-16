package com.roadscanner.dao.qna;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.roadscanner.domain.qna.AnswerVO;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations =
        {
                "file:src/main/webapp/WEB-INF/root-context.xml",
                "file:src/main/resources/mybatis-config.xml"
        })
@Transactional
public class AnswerDAOTest {

    @Autowired
    private AnswerDAO dao;

    AnswerVO answer;
    private Long no;

    // 메서드 수행 전 답변 등록
    @Before
    public void setUp() throws Exception {

        answer = new AnswerVO(229L, "admin", "답변 등록 테스트");

        dao.save(answer);

    } // add()


    // 삭제
    @Test
    //@Ignore
    public void delete() {

        no = answer.getNo();  // answer의 no값 가져오기
        dao.delete(no);

        // 삭제 후 확인
        AnswerVO deletedAnswer = dao.findByNo(no);

    } // delete()


    // 수정
    @Test
    //@Ignore
    public void update() {
    	
        no = answer.getNo();
        
        // 등록 데이터 조회
        AnswerVO getVO = dao.findByNo(no);
        
        // 데이터 비교
        isSameData(getVO, answer);
        
        // 데이터 수정
        String updateStr = "_New";
        getVO.setContent(answer.getContent() + updateStr);
        
        dao.update(getVO); // 업데이트 실행
        
        // 다시 조회해서 비교
        AnswerVO resultVO = dao.findByNo(no);
        
        // 수정된 데이터 비교
        isSameData(getVO, resultVO);
        
    } // update()


    // 조회
    @Test
    //@Ignore
    public void findByNo() throws Exception {
    	
        no = answer.getNo();
        AnswerVO outVO = dao.findByNo(no);
        
        // 조회 데이터 비교
        isSameData(outVO, answer);
        
    } // findByNo()


    // 조회된 데이터와 동일한지 비교하는 메서드
    public void isSameData(AnswerVO outVO, AnswerVO answer) {

        assertEquals(outVO.getId(), answer.getId());
        assertEquals(outVO.getContent(), answer.getContent());

    } // isSameData()

    @Test
    @Ignore
    public void bean() {
        assertNotNull(dao);
    } // bean()

} // class end
