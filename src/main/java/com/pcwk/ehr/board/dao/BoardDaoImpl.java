package com.pcwk.ehr.board.dao;

import java.sql.SQLException;
import java.util.List;

import org.apache.ibatis.mapping.BoundSql;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.pcwk.ehr.board.domain.BoardVO;
import com.pcwk.ehr.board.domain.DTO;
import com.pcwk.ehr.board.domain.PcwkLoger;
/**
 * 1.doDelete
 * 2.doSave
 * 3.doSelectOne
 * 
 * 4.doUpdate
 * 
 * 5.doRetrieve
 * @author user
 *
 */
@Repository
public class BoardDaoImpl implements BoardDao,PcwkLoger {
	final String NAMESPACE = "com.pcwk.ehr.board";
	final String DOT       = ".";
	 
	@Autowired
	SqlSessionTemplate sqlSessionTemplate;//DB 연결객체
	
	@Override
	public List<BoardVO> doRetrieve(BoardVO inVO) throws SQLException {
		LOG.debug("┌──────────────────────────────┐");
		LOG.debug("│doRetrieve                    │");
		LOG.debug("│inVO                          │"+inVO);
		LOG.debug("│statement                     │"+NAMESPACE+DOT+"doRetrieve");
		LOG.debug("└──────────────────────────────┘");	
		return sqlSessionTemplate.selectList(NAMESPACE+DOT+"doRetrieve", inVO);
	}
	
	public int doUpdateReadCnt(BoardVO inVO) throws SQLException {
		LOG.debug("┌──────────────────────────────┐");
		LOG.debug("│doSave                        │");
		LOG.debug("│inVO                          │"+inVO);
		LOG.debug("│statement                     │"+NAMESPACE+DOT+"doUpdateReadCnt");
		LOG.debug("└──────────────────────────────┘");			
		
		return sqlSessionTemplate.update(NAMESPACE+DOT+"doUpdateReadCnt", inVO);
	}
	
	@Override
	public int doSave(BoardVO inVO) throws SQLException {
		LOG.debug("┌──────────────────────────────┐");
		LOG.debug("│doSave                        │");
		LOG.debug("│inVO                          │"+inVO);
		LOG.debug("│statement                     │"+NAMESPACE+DOT+"doSave");
		LOG.debug("└──────────────────────────────┘");	
		BoundSql boundSql  =sqlSessionTemplate.getConfiguration().getMappedStatement(NAMESPACE+DOT+"doSave").getBoundSql(inVO);
		String query = boundSql.getSql(); 
		LOG.debug("│query                          │"+query);
		return sqlSessionTemplate.insert(NAMESPACE+DOT+"doSave", inVO);
	}

	@Override
	public int doDelete(BoardVO inVO) throws SQLException {
		LOG.debug("┌──────────────────────────────┐");
		LOG.debug("│doDelete                      │");
		LOG.debug("│inVO                          │"+inVO);
		LOG.debug("│statement                     │"+NAMESPACE+DOT+"doDelete");
		LOG.debug("└──────────────────────────────┘");	
		return sqlSessionTemplate.delete(NAMESPACE+DOT+"doDelete", inVO);
	}

	@Override
	public int doUpdate(BoardVO inVO) throws SQLException {
		LOG.debug("┌──────────────────────────────┐");
		LOG.debug("│doUpdate                      │");
		LOG.debug("│inVO                          │"+inVO);
		LOG.debug("│statement                     │"+NAMESPACE+DOT+"doUpdate");
		LOG.debug("└──────────────────────────────┘");	
		return sqlSessionTemplate.update(NAMESPACE+DOT+"doUpdate", inVO);
	}

	@Override
	public BoardVO doSelectOne(BoardVO inVO) throws SQLException {
		LOG.debug("┌──────────────────────────────┐");
		LOG.debug("│doSelectOne                   │");
		LOG.debug("│inVO                          │"+inVO);
		LOG.debug("│statement                     │"+NAMESPACE+DOT+"doSelectOne");
		LOG.debug("└──────────────────────────────┘");	
		return sqlSessionTemplate.selectOne(NAMESPACE+DOT+"doSelectOne", inVO);
	}



}
