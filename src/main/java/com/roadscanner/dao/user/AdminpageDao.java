package com.roadscanner.dao.user;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.roadscanner.domain.user.MemberVO;


public interface AdminpageDao {
	
	 public List<MemberVO> list() throws Exception;
	 
	 public int count() throws Exception;
	 
	 public List<MemberVO> listPage(int dpPost, int postNum) throws Exception;
	 
	 
	 public List<MemberVO> member(@Param("dpPost") int dpPost, @Param("postNum") int postNum, @Param("keyword") String keyword) throws Exception;
 
	 public int member_searchCntBox(String keyword) throws Exception;
	 
	 
	 public List<MemberVO> admin(@Param("dpPost") int dpPost, @Param("postNum") int postNum, @Param("keyword") String keyword, @Param("exclude") String exclude) throws Exception;
 
	 public int admin_searchCntBox(@Param("keyword") String keyword, @Param("exclude") String exclude) throws Exception;

	 
	 public List<MemberVO> banned(@Param("dpPost") int dpPost, @Param("postNum") int postNum, @Param("keyword") String keyword) throws Exception;

	 public int banned_searchCntBox(String keyword) throws Exception;
	 
}