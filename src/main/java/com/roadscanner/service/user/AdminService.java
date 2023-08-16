package com.roadscanner.service.user;

import java.util.List;

import com.roadscanner.domain.user.MemberVO;

public interface AdminService {

	// 리스팅
	public List<MemberVO> list() throws Exception;
	
	//게시물 총 갯수
	public int count() throws Exception;
	
	public List<MemberVO> listPage(int displayPost, int postNum) throws Exception;
	
	
	public List<MemberVO> member(int dpPost, int postNum, String keyword) throws Exception;
	
	public int member_searchCntBox(String keyword)throws Exception;
	

	public List<MemberVO> admin(int dpPost, int postNum, String keyword, String exclude) throws Exception;
	
	public int admin_searchCntBox(String keyword, String exclude)throws Exception;

	
	public List<MemberVO> banned(int dpPost, int postNum, String keyword) throws Exception;
	
	public int banned_searchCntBox(String keyword)throws Exception;


}