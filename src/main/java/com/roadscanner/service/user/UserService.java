package com.roadscanner.service.user;

import java.sql.SQLException;

import com.roadscanner.domain.user.MemberVO;

public interface UserService {
	
	/**
	 * id를 사용하여 회원정보 출력
	 * @param user
	 * @return 
	 * @throws SQLException
	 */
	public MemberVO selectUser(MemberVO user) throws SQLException;	
	
	/**
	 * 로그인
	 * @param user
	 * @return 10: id 없음, 20: 비밀번호 오류, 30: 로그인 성공, 40: 정지된 회원
	 * @throws SQLException
	 */
	public int doLogin(MemberVO user) throws SQLException;
	
	/**
	 * 회원가입
	 * @param user
	 * @return 1: 성공 , 0: 실패
	 * @throws SQLException
	 */
	public int register(MemberVO user) throws SQLException;
	
	/**
	 * 아이디 중복 검사
	 * @param user
	 * @return 1: id 존재 , 0: id 없음 
	 * @throws SQLException
	 */
	public int doIdDuplCheck(MemberVO user) throws SQLException;
	
	/**
	 * 아이디 중복 검사
	 * @param user
	 * @return 1: id 존재 , 0: id 없음 
	 * @throws SQLException
	 */
	public int doEmailDuplCheck(MemberVO user) throws SQLException;
	
	/**
	 * 아이디 여부 확인 id 반환
	 * @param user
	 * @return 1: ID 있음, -1: ID 없음
	 * @throws SQLException
	 */
	public String doSearchId(MemberVO user) throws SQLException;
	
	/**
	 * 비밀번호 중복 확인 후, 비밀번호 수정
	 * @param user
	 * @return 1: 중복 PW 있음, 0: 중복 PW 없음
	 * @throws SQLException
	 */
	public String doSearchPw(MemberVO user) throws SQLException;
	
	/**
	 * 회원정보 변경
	 * @param user
	 * @return 1: 성공 , -1: 실패 
	 * @throws SQLException
	 */
	public int doChangeInfo(MemberVO user) throws SQLException;

	/**
	 * 회원탈퇴
	 * @param user
	 * @return 1: 탈퇴 성공 / 0: 탈퇴 실패
	 * @throws SQLException
	 */
	public int doWithdraw(MemberVO user) throws SQLException;
	
	public int delete(MemberVO user) throws SQLException;
	/**
	 * 등급 변경
	 * @param user
	 * @return 등급 3으로 변경 -> 아이디 정지 
	 * @throws SQLException
	 */
	public int forbiddenGrade(MemberVO user) throws SQLException;
	
	/**
	 * 등급 변경
	 * @param user
	 * @return 등급 1로 변경 -> 정지 해제
	 * @throws SQLException
	 */
	public int clearGrade(MemberVO user) throws SQLException;
	
	/**
	 * 비밀번호 재설정 전용 수정
	 * @param user
	 * @return 1: 성공 , -1: 실패 
	 * @throws SQLException
	 */
	public int changePw(MemberVO user) throws SQLException;
		
}
