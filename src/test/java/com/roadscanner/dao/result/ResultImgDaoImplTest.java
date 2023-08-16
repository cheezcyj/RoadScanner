package com.roadscanner.dao.result;

import static org.junit.Assert.*;

import java.sql.SQLException;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.roadscanner.cmn.PcwkLogger;
import com.roadscanner.domain.result.ResultImgVO;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { 
		"file:src/main/webapp/WEB-INF/root-context.xml",
		"file:src/main/resources/mybatis-config.xml" 
		})
@Transactional
public class ResultImgDaoImplTest implements PcwkLogger {
	
	@Autowired
	ApplicationContext context;
	
	@Autowired
	ResultImgDao dao;
	
	// 테스트를 수행하는데 필요한 정보나 오브젝트: fixture
	ResultImgVO resultVO1;
	ResultImgVO resultVO2;
	ResultImgVO resultVO3;
	
	
	@Before
	public void setUp() throws Exception {
		
		resultVO1 = new ResultImgVO(1, "", "十자형 교차로", "");
		resultVO2 = new ResultImgVO(2, "", "T자형 교차로", "");
		resultVO3 = new ResultImgVO(3, "", "Y자형 교차로", "");
	}
	
	@Test
	//@Ignore
	public void doSelectOne() throws SQLException {
		LOG.debug("┌────────────────────────┐");
		LOG.debug("│      doSelectOne()     │");
		LOG.debug("└────────────────────────┘");

		ResultImgVO outVO1 = dao.getResultImg(resultVO1);
		ResultImgVO outVO2 = dao.getResultImg(resultVO2);
		ResultImgVO outVO3 = dao.getResultImg(resultVO3);

		isSameData(outVO1, resultVO1);
		isSameData(outVO2, resultVO2);
		isSameData(outVO3, resultVO3);
	}
	
	// 테스트 검증 자동화
	public void isSameData(ResultImgVO outVO, ResultImgVO inVO) {
		assertEquals(outVO.getNo(), inVO.getNo());
		assertEquals(outVO.getContent(), inVO.getContent());
	}
	
	@Test
	@Ignore
	public void bean() {
		LOG.debug("┌────────────────────────┐");
		LOG.debug("│          bean          │");
		LOG.debug("│          dao           │" + dao);
		LOG.debug("└────────────────────────┘");

		assertNotNull(dao); // dao 객체가 잘 만들어졌는지 확인
	}

}