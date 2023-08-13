package com.roadscanner.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.SQLException;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.roadscanner.dao.user.UserDao;
import com.roadscanner.domain.MemberVO;

@RunWith(SpringJUnit4ClassRunner.class)												//스프링 테스트 컨텍스 프레임워크의 JUnit 확장 기능 지정
@ContextConfiguration(locations = {
		"file:src/main/webapp/WEB-INF/root-context.xml",
		"file:src/main/webapp/WEB-INF/servlet-context.xml"})	//테스트 컨텍스트가 자동으로 만들어줄 applicationContext 위치
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MemberDaoTest {
	
	@Autowired
	UserDao dao;
	
	MemberVO vo01;
	MemberVO vo02;
	MemberVO vo03;
	
	MemberVO search;
	
	@Before
	public void setUp() throws Exception {
		vo01 = new MemberVO("DaoTest01", "password01", "test01@gmail.com", 1, 141);
		vo02 = new MemberVO("DaoTest02", "password02", "test02@gmail.com", 1, 142);
		vo03 = new MemberVO("DaoTest03", "password03", "test03@gmail.com", 1, 143);
		
		search = new MemberVO("DaoTest", "password", "test@gmail.com", 1 ,141);
	}
	
	
	@Test
	public void daoTest() throws SQLException {
		
		// Delete
		dao.deleteOne(vo01);
		dao.deleteOne(vo02);
		dao.deleteOne(vo03);
		
		// Create
		dao.addUser(vo01);
		dao.addUser(vo02);
		dao.addUser(vo03);
		
		// Retrieve
		MemberVO out01 = dao.selectOne(vo01);
		CompareData(out01, vo01);
		System.out.println("dao test out value : "+out01);
		System.out.println("dao test vo value : "+vo01);
		
		// Update

		String updatestr = "anotherpassword"; 
		out01.setPassword(out01.getPassword()+updatestr); 
		dao.updateUser(out01);
		
		MemberVO update01 = dao.selectOne(vo01);
		
		CompareData(update01, out01);
		
	}
	
	private void CompareData(MemberVO out01, MemberVO vo01) {
		assertEquals(out01.getId(), vo01.getId());
		assertEquals(out01.getPassword(), vo01.getPassword());
		assertEquals(out01.getEmail(), vo01.getEmail());
		assertEquals(out01.getGrade(), vo01.getGrade());
	}
	
	@Test
	public void Bean() {
		System.out.println("= = = = = = = = = = = = = = = = = =");
		System.out.println("bean()");
		System.out.println("dao : "+dao);
		System.out.println("= = = = = = = = = = = = = = = = = =");
		
		assertNotNull(dao);
	}

}
