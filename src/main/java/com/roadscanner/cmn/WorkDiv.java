package com.roadscanner.cmn;

import java.sql.SQLException;
import java.util.List;

/**
 * 
 * @author user
 *
 * @param <T>
 */
public interface WorkDiv<T> {
	//CRUD : doSave().doDelete(),doUpdate(),doSelectOne(), doRetrieve()
	
	/**
	 * 등록
	 * @param inVO
	 * @return
	 * @throws SQLException
	 */
	public abstract int doSave(T inVO) throws SQLException;
	

	/**
	 * 삭제
	 * @param inVO
	 * @return
	 * @throws SQLException
	 */
	int doDelete(T inVO) throws SQLException;

	/**
	 * 수정
	 * @param inVO
	 * @return
	 * @throws SQLException
	 */
	int doUpdate(T inVO) throws SQLException;

	/**
	 * 단건 조회
	 * @param inVO
	 * @return
	 * @throws SQLException
	 */
	T doSelectOne(T inVO) throws SQLException;
	

	/**
	 * 목록 조회
	 * @param inVO
	 * @return
	 * @throws SQLException
	 */
	List<T> doRetrueve(DTO inVO) throws SQLException;
}
