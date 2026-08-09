package com.roadscanner.dao.user;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import com.roadscanner.domain.user.MemberVO;

@Repository
public class AdminpageDaoImpl implements AdminpageDao {
	private static final String NAMESPACE = "com.roadscanner.dao.user.AdminpageDao";

	private final SqlSessionTemplate sqlSessionTemplate;

	@Autowired
	public AdminpageDaoImpl(SqlSessionTemplate sqlSessionTemplate) {
		this.sqlSessionTemplate = sqlSessionTemplate;
	}
	
	@Override
	public List<MemberVO> member(int dpPost, int postNum, String keyword) throws Exception {
		 Map<String, Object> data = new HashMap<String, Object>();
		  
		  data.put("dpPost", dpPost);
		  data.put("postNum", postNum);
		  data.put("keyword", keyword);
		
		return sqlSessionTemplate.selectList(NAMESPACE + ".member", data);
		
	}

	@Override
	public int member_searchCntBox(String keyword) throws Exception {
		 	Map<String, Object> data = new HashMap<String, Object>();
		 	
		 	data.put("keyword", keyword);
		 	
		  return sqlSessionTemplate.selectOne(NAMESPACE + ".member_searchCntBox", data);
		  
	}
	
	@Override
	public List<MemberVO> admin(int dpPost, int postNum, String keyword, String exclude ) throws Exception {
		 Map<String, Object> data = new HashMap<String, Object>();
		  
		 data.put("dpPost", dpPost);
		 data.put("postNum", postNum);
		 data.put("keyword", keyword);
		 data.put("exclude", exclude);
		
		return sqlSessionTemplate.selectList(NAMESPACE + ".admin", data);
		
	}

	@Override
	public int admin_searchCntBox(String keyword,String exclude) throws Exception {
		 	Map<String, Object> data = new HashMap<String, Object>();
		 	
		 	data.put("keyword", keyword);	
		 	data.put("exclude", exclude);
		 	
		  return sqlSessionTemplate.selectOne(NAMESPACE + ".admin_searchCntBox", data);	  
	}

	@Override
	public List<MemberVO> banned(int dpPost, int postNum, String keyword) throws Exception {
		Map<String, Object> data = new HashMap<String, Object>();
		  
		  data.put("dpPost", dpPost);
		  data.put("postNum", postNum);
		  data.put("keyword", keyword);
		
		return sqlSessionTemplate.selectList(NAMESPACE + ".banned", data);
	}

	@Override
	public int banned_searchCntBox(String keyword) throws Exception {
		Map<String, Object> data = new HashMap<String, Object>();
	 	
	 	data.put("keyword", keyword);	
	 	
	  return sqlSessionTemplate.selectOne(NAMESPACE + ".banned_searchCntBox", data);
	}

	

}
