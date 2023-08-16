package com.roadscanner.service.result;

import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.roadscanner.cmn.PcwkLogger;
import com.roadscanner.dao.result.ResultImgDao;
import com.roadscanner.domain.result.ResultImgVO;

@Service
public class ResultImgServiceImpl implements PcwkLogger, ResultImgService {
	
	@Autowired
	@Qualifier(value = "resultImgDaoImpl")
	ResultImgDao dao;
	
	//@Autowired
	ResultImgVO resultVO;

	@Override
	public ResultImgVO getResultImg(ResultImgVO inVO) throws SQLException {
		
		return dao.getResultImg(inVO);
	}

}