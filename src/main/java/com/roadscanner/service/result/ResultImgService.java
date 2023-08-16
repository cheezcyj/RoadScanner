package com.roadscanner.service.result;

import java.sql.SQLException;

import com.roadscanner.domain.result.ResultImgVO;

public interface ResultImgService {

	ResultImgVO getResultImg(ResultImgVO inVO) throws SQLException;
}
