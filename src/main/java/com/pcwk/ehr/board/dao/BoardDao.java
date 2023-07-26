package com.pcwk.ehr.board.dao;

import java.sql.SQLException;

import com.pcwk.ehr.board.domain.BoardVO;
import com.pcwk.ehr.board.domain.WorkDiv;

public interface BoardDao extends WorkDiv<BoardVO> {

	public int doUpdateReadCnt(BoardVO inVO) throws SQLException;
}
