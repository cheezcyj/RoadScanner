package com.roadscanner.service.user;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.roadscanner.dao.user.AdminpageDao;
import com.roadscanner.domain.MemberVO;


@Service
public class AdminServiceImpl implements AdminService{
	
	@Inject
	private AdminpageDao adminpageDao; 
	
	@Override
	public List<MemberVO> list() throws Exception {
		return adminpageDao.list();
	}
	
	@Override
	public int count() throws Exception {
		return adminpageDao.count();
	}
	
	@Override
	public List<MemberVO> listPage(int displayPost, int postNum) throws Exception {
	 return adminpageDao.listPage(displayPost, postNum);
	}

	@Override
	public List<MemberVO> listPageSearchBox(int displayPost, int postNum,String keyword) throws Exception {
		return adminpageDao.listPageSearchBox(displayPost, postNum, keyword);
	}
	
	@Override

	public int searchCountBox( String keyword) throws Exception {
		
		return adminpageDao.searchCountBox(keyword);
	}
	
	@Override
	public List<MemberVO> listPageSearchBox2(int displayPost2, int postNum2,String nekeyword,String keyword2) throws Exception {
		return adminpageDao.listPageSearchBox2(displayPost2, postNum2,nekeyword,
												keyword2);
	}
	
	@Override
	public int searchCountBox2(String nekeyword,String keyword2) throws Exception {
		return adminpageDao.searchCountBox2(
				nekeyword,keyword2);
	}

	@Override
	public List<MemberVO> listPageSearchBox3(int displayPost3, int postNum3, String keyword3) throws Exception {

		return adminpageDao.listPageSearchBox3(displayPost3, postNum3, keyword3);
	}

	@Override
	public int searchCountBox3(String keyword3) throws Exception {
		
		return adminpageDao.searchCountBox3(keyword3);
	}


}
