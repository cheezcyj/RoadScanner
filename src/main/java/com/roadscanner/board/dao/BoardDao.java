package com.roadscanner.board.dao;

import java.sql.SQLException;

import com.roadscanner.board.domain.BoardVO;
import com.roadscanner.board.domain.WorkDiv;

public interface BoardDao extends WorkDiv<BoardVO> {

	public int doUpdateReadCnt(BoardVO inVO) throws SQLException;
}
