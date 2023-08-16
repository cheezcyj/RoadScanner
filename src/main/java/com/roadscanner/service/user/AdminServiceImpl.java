package com.roadscanner.service.user;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.roadscanner.dao.user.AdminpageDao;
import com.roadscanner.domain.user.MemberVO;


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
	public List<MemberVO> member(int dpPost, int postNum, String keyword) throws Exception {
		
		return adminpageDao.member(dpPost, postNum, keyword);
	}
	
	@Override

	public int member_searchCntBox(String keyword) throws Exception {
		
		return adminpageDao.member_searchCntBox(keyword);
	}
	
	@Override
	public List<MemberVO> admin(int dpPost, int postNum, String keyword, String exclude) throws Exception {
		
		return adminpageDao.admin(dpPost, postNum, keyword, exclude);
	}
	
	@Override
	public int admin_searchCntBox(String keyword,String exclude) throws Exception {
		
		return adminpageDao.admin_searchCntBox(keyword,exclude);
	}

	@Override
	public List<MemberVO> banned(int dpPost, int postNum, String keyword) throws Exception {

		return adminpageDao.banned(dpPost, postNum, keyword);
	}

	@Override
	public int banned_searchCntBox(String keyword) throws Exception {
		
		return adminpageDao.banned_searchCntBox(keyword);
	}


}
