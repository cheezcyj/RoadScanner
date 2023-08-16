package com.roadscanner.dao.upload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.SQLException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.roadscanner.cmn.PcwkLogger;
import com.roadscanner.domain.upload.FileUploadVO;

@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { 
		"file:src/main/webapp/WEB-INF/root-context.xml",
		"file:src/main/resources/mybatis-config.xml"
})
@Transactional
public class FileUploadDaoImplTest implements PcwkLogger {

	@Autowired
	ApplicationContext context;

	@Autowired
	FileUploadDao dao;

	FileUploadVO search;

	// 테스트를 수행하는데 필요한 정보나 오브젝트: fixture
	FileUploadVO uploadVO1;
	FileUploadVO uploadVO2;
	FileUploadVO uploadVO3;

	@Before
	public void setUp() throws Exception {

		uploadVO1 = new FileUploadVO(11, "a", 10, "20230802", "230731095807_dog.jpg", "url01", 700, 0, 0, 0);
		uploadVO2 = new FileUploadVO(12, "a", 10, "20230202", "230731095807_cat.jpg", "url02", 800, 0, 0, 0);
		uploadVO3 = new FileUploadVO(13, "a", 10, "20230402", "230731095807_cow.jpg", "url03", 900, 0, 0, 0);

		search = new FileUploadVO(10, "a", 10, "20230802", "230731095807_dog.jpg", "url01", 700, 0, 0, 0);
	}

	/*
	 * 피드백 월별 그래프
	 */
	@Test
	public void monthlyFeedback() throws SQLException {
		LOG.debug("┌──────────────────────┐");
		LOG.debug("│   monthlyFeedback()  │");
		LOG.debug("└──────────────────────┘");

		List<FileUploadVO> list = dao.monthlyFeedback(uploadVO1);
		
		LOG.debug("------------------------------");
		for (FileUploadVO vo : list) {
			LOG.debug("날짜: " + vo.getUploadDate() + ", 인식오류: " + vo.getU1() + ", 결과오류: " + vo.getU2());
		}
		LOG.debug("------------------------------");
	}

	/*
	 * 피드백 누적 그래프, 표
	 */
	@Test
	public void totalFeedback() throws SQLException {
		LOG.debug("┌───────────────────────┐");
		LOG.debug("│    totalFeedback()    │");
		LOG.debug("└───────────────────────┘");

		FileUploadVO outVO1 = dao.totalFeedback(uploadVO1);

		assertNotNull(outVO1); // 반환된 객체가 null이 아닌지 확인

		// 결과 출력
		LOG.debug("------------------------------");
		LOG.debug("누적인식오류: " + outVO1.getU1());
		LOG.debug("누적결과오류: " + outVO1.getU2());
		LOG.debug("------------------------------");
	}

	/*
	 * 수정
	 */
	@Test
	public void update() throws SQLException {
		LOG.debug("┌───────────────────────┐");
		LOG.debug("│        update()       │");
		LOG.debug("└───────────────────────┘");

		// 삭제
		dao.doDelete(uploadVO1);
		dao.doDelete(uploadVO2);
		dao.doDelete(uploadVO3);

		// 등록
		dao.doSave(uploadVO1);
		dao.doSave(uploadVO2);
		dao.doSave(uploadVO3);

		// 조회
		FileUploadVO outVO1 = dao.doSelectOne(uploadVO1);
		FileUploadVO outVO2 = dao.doSelectOne(uploadVO2);
		FileUploadVO outVO3 = dao.doSelectOne(uploadVO3);

		// 조회 데이터 한 건 수정
		String str = "_u";

		outVO1.setUrl(outVO1.getUrl() + str);
		outVO1.setCategory(20);
		outVO1.setChecked(1);
		outVO1.setU1(1);
		outVO1.setU2(1);

		int flag = dao.doUpdate(outVO1);
		assertEquals(flag, 1);

		// 수정 데이터 조회
		FileUploadVO upVO1 = dao.doSelectOne(outVO1);
		
		// 조회 데이터 비교
		isSameData(upVO1, outVO1);
		LOG.debug(upVO1);
		LOG.debug(outVO1);
		
	}

	/*
	 * 카테고리별 목록 조회
	 */
	@Test
	public void doRetrieveByCategory() throws SQLException {
		LOG.debug("┌───────────────────────┐");
		LOG.debug("│      doRetrieve()     │");
		LOG.debug("└───────────────────────┘");

		// 삭제
		dao.doDelete(uploadVO1);
		dao.doDelete(uploadVO2);
		dao.doDelete(uploadVO3);

		// 등록
		dao.doSave(uploadVO1);
		dao.doSave(uploadVO2);
		dao.doSave(uploadVO3);

		// 목록 조회
		search.setPageSize(9); // 페이지 사이즈
		search.setPageNo(1); // 페이지 번호
		
		List<FileUploadVO> list = dao.doRetrieveByCategory(search);
		for (FileUploadVO vo : list) {
			LOG.debug(vo);
		}
	}	
	
	/*
	 * 목록 조회
	 */
	@Test
	public void doRetrieve() throws SQLException {
		LOG.debug("┌───────────────────────┐");
		LOG.debug("│      doRetrieve()     │");
		LOG.debug("└───────────────────────┘");

		// 삭제
		dao.doDelete(uploadVO1);
		dao.doDelete(uploadVO2);
		dao.doDelete(uploadVO3);

		// 등록
		dao.doSave(uploadVO1);
		dao.doSave(uploadVO2);
		dao.doSave(uploadVO3);

		// 목록 조회
		search.setPageSize(9); // 페이지 사이즈
		search.setPageNo(1); // 페이지 번호

		List<FileUploadVO> list = dao.doRetrieve(search);
		for (FileUploadVO vo : list) {
			LOG.debug(vo);
		}

	}

	/*
	 * 삭제, 등록, 단건조회
	 */
	@Test
	public void addAndGet() throws SQLException {
		LOG.debug("┌────────────────────────┐");
		LOG.debug("│       addAndGet()      │");
		LOG.debug("└────────────────────────┘");

		// 삭제
		dao.doDelete(uploadVO1);
		dao.doDelete(uploadVO2);
		dao.doDelete(uploadVO3);

		// 등록
		dao.doSave(uploadVO1);
		dao.doSave(uploadVO2);
		dao.doSave(uploadVO3);

		// 조회
		FileUploadVO outVO1 = dao.doSelectOne(uploadVO1);
		FileUploadVO outVO2 = dao.doSelectOne(uploadVO2);
		FileUploadVO outVO3 = dao.doSelectOne(uploadVO3);

		// 조회 데이터 비교
		isSameData(outVO1, uploadVO1);
		isSameData(outVO2, uploadVO2);
		isSameData(outVO3, uploadVO3);

	}

	
	@Test
	public void doDelete() throws SQLException {
		LOG.debug("┌────────────────────────┐");
		LOG.debug("│       doDelete()       │");
		LOG.debug("└────────────────────────┘");

		dao.doDelete(uploadVO1);
		dao.doDelete(uploadVO2);
		dao.doDelete(uploadVO3);

	}

	@Test
	public void doSave() throws SQLException {
		LOG.debug("┌────────────────────────┐");
		LOG.debug("│        doSave()        │");
		LOG.debug("└────────────────────────┘");

		dao.doSave(uploadVO1);
		dao.doSave(uploadVO2);
		dao.doSave(uploadVO3);

	}

	// 테스트 검증 자동화
	public void isSameData(FileUploadVO outVO, FileUploadVO inVO) {
		assertEquals(outVO.getId(), inVO.getId());
		assertEquals(outVO.getCategory(), inVO.getCategory());
		assertEquals(outVO.getName(), inVO.getName());
		assertEquals(outVO.getUrl(), inVO.getUrl());
		assertEquals(outVO.getFileSize(), inVO.getFileSize());
		assertEquals(outVO.getChecked(), inVO.getChecked());
		assertEquals(outVO.getU1(), inVO.getU1());
		assertEquals(outVO.getU2(), inVO.getU2());
	}

	@Test
	public void bean() {
		LOG.debug("┌────────────────────────┐");
		LOG.debug("│          bean          │");
		LOG.debug("│          dao           │" + dao);
		LOG.debug("└────────────────────────┘");

		assertNotNull(dao); // dao 객체가 잘 만들어졌는지 확인
	}
}