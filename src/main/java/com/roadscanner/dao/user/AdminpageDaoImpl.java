package com.roadscanner.dao.user;

import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import com.roadscanner.domain.MemberVO;

@Repository
public class AdminpageDaoImpl implements AdminpageDao {
	@Inject
	private SqlSessionTemplate sqlSessionTemplate ;
	private static String namespace = "adminpage";
	
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
	public List<MemberVO> listPage(int displayPost, int postNum) throws Exception {

	 HashMap<String,Integer> data = new HashMap<String,Integer>();
	  
	 data.put("displayPost", displayPost);
	 data.put("postNum", postNum);
	  
	 return sqlSessionTemplate.selectList(namespace + ".listPage", data);
	 
	}

	@Override
	public List<MemberVO> listPageSearchBox(
			int displayPost, int postNum, String keyword) throws Exception {
		 HashMap<String, Object> data = new HashMap<String, Object>();
		  
		  data.put("displayPost", displayPost);
		  data.put("postNum", postNum);
		  data.put("keyword", keyword);
		
		return sqlSessionTemplate.selectList(namespace + ".listPageSearchBox", data);
		
	}

	@Override
	public int searchCountBox(String keyword) throws Exception {
		 	HashMap<String, Object> data = new HashMap<String, Object>();
		 	
		 	System.out.println("keyword:"+keyword);
		 	
		 	data.put("keyword", keyword);	
		  return sqlSessionTemplate.selectOne(namespace + ".searchCountBox", data);
		  
	}
	
	@Override
	public List<MemberVO> listPageSearchBox2(
			int displayPost2, int postNum2,String nekeyword, String keyword2) throws Exception {
		 HashMap<String, Object> data2 = new HashMap<String, Object>();
		  
		  data2.put("displayPost2", displayPost2);
		  data2.put("postNum2", postNum2);
		  data2.put("nekeyword", nekeyword);
		  data2.put("keyword2", keyword2);
		
		return sqlSessionTemplate.selectList(namespace + ".listPageSearchBox2", data2);
		
	}

	@Override
	public int searchCountBox2(String nekeyword,String keyword2) throws Exception {
		 	HashMap<String, Object> data2 = new HashMap<String, Object>();
		 	
		 	System.out.println("keyword2:"+keyword2);
		 	data2.put("nekeyword", nekeyword);
		 	data2.put("keyword2", keyword2);	
		  return sqlSessionTemplate.selectOne(namespace + ".searchCountBox2", data2);
		  
	}

	@Override
	public List<MemberVO> listPageSearchBox3(int displayPost3, int postNum3, String keyword3) throws Exception {
		HashMap<String, Object> data3 = new HashMap<String, Object>();
		  
		  data3.put("displayPost3", displayPost3);
		  data3.put("postNum3", postNum3);
		  data3.put("keyword3", keyword3);
		
		return sqlSessionTemplate.selectList(namespace + ".listPageSearchBox3", data3);
	}

	@Override
	public int searchCountBox3(String keyword3) throws Exception {
		HashMap<String, Object> data3 = new HashMap<String, Object>();
	 	
	 	System.out.println("keyword3:"+keyword3);
	 	
	 	data3.put("keyword3", keyword3);	
	  return sqlSessionTemplate.selectOne(namespace + ".searchCountBox3", data3);
	}

	

}
