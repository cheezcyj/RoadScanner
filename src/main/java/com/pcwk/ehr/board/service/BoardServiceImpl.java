package com.pcwk.ehr.board.service;

import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pcwk.ehr.board.dao.BoardDao;
import com.pcwk.ehr.board.domain.BoardVO;
import com.pcwk.ehr.board.domain.DTO;

@Service
public class BoardServiceImpl implements BoardService {

	@Autowired
	BoardDao dao; 
	
	@Override
	public int doSave(BoardVO inVO) throws SQLException {
		return dao.doSave(inVO);
	}

	@Override
	public int doDelete(BoardVO inVO) throws SQLException {
		return dao.doDelete(inVO);
	}

	@Override
	public int doUpdate(BoardVO inVO) throws SQLException {
		return dao.doUpdate(inVO);
	}

	@Override
	public BoardVO doSelectOne(BoardVO inVO) throws SQLException {
		//1. 단건조회
		//2. 조회 count증가
		BoardVO vo = dao.doSelectOne(inVO);
		
		if(null !=vo ) {
			dao.doUpdateReadCnt(inVO);
		}
		return vo;
	}

	@Override
	public List<BoardVO> doRetrieve(BoardVO inVO) throws SQLException {
		return dao.doRetrieve(inVO);
	}

}
