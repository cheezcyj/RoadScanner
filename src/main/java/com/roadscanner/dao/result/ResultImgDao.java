package com.roadscanner.dao.result;

import java.sql.SQLException;

import com.roadscanner.domain.result.ResultImgVO;

public interface ResultImgDao {

	ResultImgVO getResultImg(ResultImgVO inVO) throws SQLException;
}
