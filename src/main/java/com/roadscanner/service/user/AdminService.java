package com.roadscanner.service.user;

import java.util.List;

import com.roadscanner.domain.user.MemberVO;

public interface AdminService {
	List<MemberVO> member(int dpPost, int postNum, String keyword) throws Exception;
	
	int member_searchCntBox(String keyword)throws Exception;
	
	List<MemberVO> admin(int dpPost, int postNum, String keyword, String exclude) throws Exception;
	
	int admin_searchCntBox(String keyword, String exclude)throws Exception;

	List<MemberVO> banned(int dpPost, int postNum, String keyword) throws Exception;
	
	int banned_searchCntBox(String keyword)throws Exception;


}
