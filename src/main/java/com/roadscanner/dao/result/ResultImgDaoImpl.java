package com.roadscanner.dao.result;

import java.sql.SQLException;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.roadscanner.cmn.PcwkLogger;
import com.roadscanner.domain.result.ResultImgVO;

@Repository("resultImgDaoImpl")
public class ResultImgDaoImpl implements PcwkLogger, ResultImgDao {

	final String NAMESPACE = "com.roadscanner.dao.result.ResultImgDao";
	final String DOT = ".";

	@Autowired
	SqlSessionTemplate sqlSessionTemplate;
	
	// Default 생성자
	public ResultImgDaoImpl() {
		
	}
	
	@Override
	public ResultImgVO getResultImg(ResultImgVO inVO) throws SQLException {
		LOG.debug("┌─────────────────────────┐");
		LOG.debug("│      getResultImg()     │");
		LOG.debug("│           inVO          │" + inVO);
		LOG.debug("│         statement       │" + NAMESPACE + DOT + "getResultImg");
		LOG.debug("└─────────────────────────┘");
		
		return sqlSessionTemplate.selectOne(NAMESPACE + DOT + "getResultImg", inVO);
	}
	
}
