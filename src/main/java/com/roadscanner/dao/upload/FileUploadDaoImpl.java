package com.roadscanner.dao.upload;

import java.sql.SQLException;
import java.util.List;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import com.roadscanner.cmn.AppLogger;
import com.roadscanner.domain.upload.FileUploadVO;

@Repository("fileUploadDaoImpl")
public class FileUploadDaoImpl implements AppLogger, FileUploadDao {

	final String NAMESPACE = "com.roadscanner.dao.upload.FileUploadDao";
	final String DOT = ".";

	@Autowired
	SqlSessionTemplate sqlSessionTemplate;

	// Default 생성자
	public FileUploadDaoImpl() {
	}

	// 피드백 월별 그래프
	@Override
	public List<FileUploadVO> monthlyFeedback(FileUploadVO inVO) throws SQLException {
		LOG.debug("┌────────────────────────┐");
		LOG.debug("│    monthlyFeedback()   │");
		LOG.debug("│        statement       │" + NAMESPACE + DOT + "monthlyFeedback");
		LOG.debug("└────────────────────────┘");

		return sqlSessionTemplate.selectList(NAMESPACE + DOT + "monthlyFeedback", inVO);

	}

	// 피드백 누적 그래프, 표
	@Override
	public FileUploadVO totalFeedback(FileUploadVO inVO) throws SQLException {
		LOG.debug("┌────────────────────────┐");
		LOG.debug("│     totalFeedback()    │");
		LOG.debug("│        statement       │" + NAMESPACE + DOT + "totalFeedback");
		LOG.debug("└────────────────────────┘");

		return sqlSessionTemplate.selectOne(NAMESPACE + DOT + "totalFeedback", inVO);
	}

	// 카테고리별 사진 목록 조회
	@Override
	public List<FileUploadVO> doRetrieveByCategory(FileUploadVO inVO) throws SQLException {
		LOG.debug("┌────────────────────────┐");
		LOG.debug("│ doRetrieveByCategory() │");
		LOG.debug("│        statement       │" + NAMESPACE + DOT + "doRetrieveByCategory");
		LOG.debug("└────────────────────────┘");
		
		return sqlSessionTemplate.selectList(NAMESPACE + DOT + "doRetrieveByCategory", inVO);
	}
	
	// 업로드된 사진 전체 목록 조회 (카테고리: 10, 20, 30)
	@Override
	public List<FileUploadVO> doRetrieve(FileUploadVO inVO) throws SQLException {
		LOG.debug("┌────────────────────────┐");
		LOG.debug("│       doRetrieve()     │");
		LOG.debug("│        statement       │" + NAMESPACE + DOT + "doRetrieve");
		LOG.debug("└────────────────────────┘");

		return sqlSessionTemplate.selectList(NAMESPACE + DOT + "doRetrieve", inVO);
	}

	// 사진 상세 조회
	@Override
	public FileUploadVO doSelectOne(FileUploadVO inVO) throws SQLException {
		LOG.debug("┌────────────────────────┐");
		LOG.debug("│      doSelectOne()     │");
		LOG.debug("│        statement       │" + NAMESPACE + DOT + "doSelectOne");
		LOG.debug("└────────────────────────┘");

		return sqlSessionTemplate.selectOne(NAMESPACE + DOT + "doSelectOne", inVO);
	}
	
	// 사진 수정
	@Override
	public int doUpdate(FileUploadVO inVO) throws SQLException {
		LOG.debug("┌────────────────────────┐");
		LOG.debug("│       doUpdate()       │");
		LOG.debug("│        statement       │" + NAMESPACE + DOT + "doUpdate");
		LOG.debug("└────────────────────────┘");
		
		return sqlSessionTemplate.update(NAMESPACE + DOT + "doUpdate", inVO);
	}
	
	// 사진 삭제
	@Override
	public int doDelete(FileUploadVO inVO) throws SQLException {
		LOG.debug("┌────────────────────────┐");
		LOG.debug("│       doDelete()       │");
		LOG.debug("│        statement       │" + NAMESPACE + DOT + "doDelete");
		LOG.debug("└────────────────────────┘");

		return sqlSessionTemplate.delete(NAMESPACE + DOT + "doDelete", inVO);
	}

	@Override
	public List<FileUploadVO> findPendingDeletes(int limit) throws SQLException {
		return sqlSessionTemplate.selectList(NAMESPACE + DOT + "findPendingDeletes", limit);
	}

	@Override
	public int restorePendingDelete(int idx, String name, int pendingChecked,
			int restoredChecked) throws SQLException {
		java.util.Map<String, Object> parameters = new java.util.HashMap<>();
		parameters.put("idx", idx);
		parameters.put("name", name);
		parameters.put("pendingChecked", pendingChecked);
		parameters.put("restoredChecked", restoredChecked);
		return sqlSessionTemplate.update(NAMESPACE + DOT + "restorePendingDelete", parameters);
	}

	// 사진 업로드
	@Override
	public int doSave(FileUploadVO inVO) throws SQLException {
		LOG.debug("┌────────────────────────┐");
		LOG.debug("│        doSave()        │");
		LOG.debug("│        statement       │" + NAMESPACE + DOT + "doSave");
		LOG.debug("└────────────────────────┘");

		return sqlSessionTemplate.insert(NAMESPACE + DOT + "doSave", inVO);
	}

}
