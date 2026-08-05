package com.roadscanner.service.result;

import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.roadscanner.cmn.AppLogger;
import com.roadscanner.dao.result.ResultImgDao;
import com.roadscanner.domain.result.ResultImgVO;

@Service
public class ResultImgServiceImpl implements AppLogger, ResultImgService {
	private final ResultImgDao dao;

	@Autowired
	public ResultImgServiceImpl(@Qualifier("resultImgDaoImpl") ResultImgDao dao) {
		this.dao = dao;
	}

	@Override
	public ResultImgVO getResultImg(ResultImgVO inVO) throws SQLException {
		
		return dao.getResultImg(inVO);
	}

}
