package com.roadscanner.dao.user;

import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import com.roadscanner.domain.user.MemberVO;

@Repository
public class AdminpageDaoImpl implements AdminpageDao {
	@Inject
	private SqlSessionTemplate sqlSessionTemplate ;
	private static String namespace = "com.roadscanner.dao.user.AdminpageDao";
	
	@Override
	public List<MemberVO> list() throws Exception {
		return sqlSessionTemplate.selectList(namespace +".list");
	}
	
	//게시물 총 갯수
	@Override
	public int count() throws Exception {
		return sqlSessionTemplate.selectOne(namespace +".count");
	}
	
	@Override
	public List<MemberVO> listPage(int dpPost, int postNum) throws Exception {

	 HashMap<String,Integer> data = new HashMap<String,Integer>();
	  
	 data.put("dpPost", dpPost);
	 data.put("postNum", postNum);
	  
	 return sqlSessionTemplate.selectList(namespace + ".listPage", data);
	 
	}

	@Override
	public List<MemberVO> member(int dpPost, int postNum, String keyword) throws Exception {
		 HashMap<String, Object> data = new HashMap<String, Object>();
		  
		  data.put("dpPost", dpPost);
		  data.put("postNum", postNum);
		  data.put("keyword", keyword);
		
		return sqlSessionTemplate.selectList(namespace + ".member", data);
		
	}

	@Override
	public int member_searchCntBox(String keyword) throws Exception {
		 	HashMap<String, Object> data = new HashMap<String, Object>();
		 	
		 	data.put("keyword", keyword);
		 	
		  return sqlSessionTemplate.selectOne(namespace + ".member_searchCntBox", data);
		  
	}
	
	@Override
	public List<MemberVO> admin(int dpPost, int postNum, String keyword, String exclude ) throws Exception {
		 HashMap<String, Object> data = new HashMap<String, Object>();
		  
		 data.put("dpPost", dpPost);
		 data.put("postNum", postNum);
		 data.put("keyword", keyword);
		 data.put("exclude", exclude);
		
		return sqlSessionTemplate.selectList(namespace + ".admin", data);
		
	}

	@Override
	public int admin_searchCntBox(String keyword,String exclude) throws Exception {
		 	HashMap<String, Object> data = new HashMap<String, Object>();
		 	
		 	data.put("keyword", keyword);	
		 	data.put("exclude", exclude);
		 	
		  return sqlSessionTemplate.selectOne(namespace + ".admin_searchCntBox", data);	  
	}

	@Override
	public List<MemberVO> banned(int dpPost, int postNum, String keyword) throws Exception {
		HashMap<String, Object> data = new HashMap<String, Object>();
		  
		  data.put("dpPost", dpPost);
		  data.put("postNum", postNum);
		  data.put("keyword", keyword);
		
		return sqlSessionTemplate.selectList(namespace + ".banned", data);
	}

	@Override
	public int banned_searchCntBox(String keyword) throws Exception {
		HashMap<String, Object> data = new HashMap<String, Object>();
	 	
	 	System.out.println("keyword : "+keyword);
	 	data.put("keyword", keyword);	
	 	
	  return sqlSessionTemplate.selectOne(namespace + ".banned_searchCntBox", data);
	}

	

}
