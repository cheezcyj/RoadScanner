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

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations =
        {
                "file:src/main/webapp/WEB-INF/root-context.xml",
                "file:src/main/resources/mybatis-config.xml"
        })
public class AnswerDAOTest {
    Logger LOG = LogManager.getLogger(getClass());

    @Autowired
    private AnswerDAO dao;

    AnswerVO answer;
    private Long no;

    @Before
    public void setUp() throws Exception {

        answer = new AnswerVO(228L, "testtest", "답변 등록 테스트");

    } // add()

//    @After
//    public void tearDown() throws Exception {
//
//        no = answer.getNo();
//
//        // 삭제
//        dao.delete(no);
//
//        // 삭제 후 확인
//        AnswerVO deletedAnswer = dao.findByNo(no);
//
//    } // delete()

    @Test
    //@Ignore
    public void update() {
    	
    	answer.setNo(228L); // 게시글 번호 설정
    	
        dao.delete(228L);
        dao.save(answer); // 먼저 답변을 등록합니다.
        
        no = answer.getNo(); // no 값을 가져옵니다.
        
        // 등록 데이터 조회
        AnswerVO getVO = dao.findByNo(answer.getNo());
        
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


    @Test
    @Ignore
    public void findByNo() throws Exception {
    	
    	dao.delete(answer.getNo());
    	dao.save(answer);
        
    	// 조회
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
