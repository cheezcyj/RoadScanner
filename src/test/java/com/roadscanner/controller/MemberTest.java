package com.roadscanner.controller;

import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.roadscanner.domain.MemberVO;
import com.roadscanner.service.user.UserService;

@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"file:src/main/webapp/WEB-INF/root-context.xml"
                                  ,"file:src/main/webapp/WEB-INF/servlet-context.xml"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING) 
public class MemberTest {
	final Logger LOG = LogManager.getLogger(getClass());
	
	@Autowired
	ApplicationContext context;
	@Autowired
	UserService userService;
		
	
	/*회원정보 받기*/
	@Test
	@Ignore
	public void selectOneUser() throws SQLException {
		MemberVO inVO = new MemberVO();
		inVO.setId("admin");
		MemberVO outVO = userService.selectUser(inVO);
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ "+inVO.getId()+"회원의 정보  : \n"+ outVO.toString()); 	// 회원 정보
		LOG.debug("└────────────────────────────────────────────────────────┘");
	}
	
	
	/* 회원 가입/탈퇴 */
	@Test
	public void addUser() throws SQLException {
		
		MemberVO user = new MemberVO();
		user.setGrade(1);
		user.setId("admin");
		user.setPassword("123123");
		user.setEmail("test@naver.com");
		user.setRegdate("sysdate");


//		int result = userService.deleteOne(user); 	//**** 회원 삭제 서비스 호출 ****//
//		LOG.debug("┌────────────────────────────────────────────────────────┐");
//		LOG.debug("│ 회원 단건 삭제  : "+ result); 	// 10: 삭제, 20: 삭제 안됨
//		LOG.debug("└────────────────────────────────────────────────────────┘");
		
		int result2 = userService.register(user);		//**** 회원 추가 서비스 호출 ****//
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ 회원 추가 : "+ result2);		// 10: 추가, 0: 추가 안됨
		LOG.debug("└────────────────────────────────────────────────────────┘");
	}
	
	/* 아이디 중복 확인 */
	@Test
	@Ignore
	public void idCheck() throws SQLException {
		MemberVO inVO = new MemberVO();
		inVO.setId("ss");
		int result = userService.doIdDuplCheck(inVO); 	//**** 회원 로그인 서비스 호출 ****//
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ 아이디 중복 여부  : "+ result); //10: 이미 id 있음, 20: 없음
		LOG.debug("└────────────────────────────────────────────────────────┘");
	}
	
	/* 아이디 찾기 */
	@Test
	@Ignore
	public void searchId() throws SQLException {
		MemberVO user = new MemberVO();
		user.setEmail("test@naver.com");
        
        String result = "-1";
        result = userService.doSearchId(user);     
        if("-1".equals(result)) {       
            LOG.debug("┌────────────────────────────────────────────────────────┐");
            LOG.debug("│ 아이디 찾기  : 이름, 이메일을 다시 확인 바람"); 
            LOG.debug("└────────────────────────────────────────────────────────┘");
        }else {        
            LOG.debug("┌────────────────────────────────────────────────────────┐");
            LOG.debug("│ 아이디 찾기  (아이디  : "+ result); // 찾고자 하는 ID
            LOG.debug("└────────────────────────────────────────────────────────┘");
        }
	}
	

	/* 비밀번호  찾기 */
	@Test
	@Ignore
	public void searchPw() throws SQLException {
		MemberVO user = new MemberVO();
		user.setId("admin");
		user.setEmail("test@naver.com");
		String result = userService.doSearchPw(user); 	
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ 비밀번호 수정 여부   : "+ result); 	
		LOG.debug("└────────────────────────────────────────────────────────┘");
	}
	
	

	/*로그인 테스트*/
	@Test
	@Ignore
	public void login() throws SQLException{
		MemberVO inVO = new MemberVO();
		inVO.setId("admin");
		inVO.setPassword("123123");
		int result = userService.doLogin(inVO); 	
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ 로그인 여부  : "+ result); //10: id 없음, 20: 비밀번호 오류, 30: 로그인 성공 
		LOG.debug("└────────────────────────────────────────────────────────┘");
	}
	

	/*수정 테스트*/
	@Test
	@Ignore
	public void changeInfo() throws SQLException{

		MemberVO inVO = new MemberVO();
		inVO.setId("admin");
		MemberVO outVO = userService.selectUser(inVO);
		
		MemberVO inVO2 = outVO;
		inVO2.setId("ohdoho");
		inVO2.setEmail("test@naver.com");
		LOG.debug("│ inVO2  : "+ inVO2.toString()); 
		
		int result = userService.doChangeInfo(inVO2); 	//**** 회원 정보 수정 서비스 호출 ****//
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ 로그인 여부  : "+ result); 
		LOG.debug("└────────────────────────────────────────────────────────┘");
	}
	
	
	
}