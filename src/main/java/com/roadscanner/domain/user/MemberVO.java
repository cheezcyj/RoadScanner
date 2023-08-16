package com.roadscanner.domain.user;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString

public class MemberVO {
	private String id; // 회원 아이디
	private String password; // 회원 비밀번호
	private String email; // 회원 이메일
	private int grade;// 회원 등급
	private String regdate;	// 가입일
	private int no;	//회원 번호(시퀀스)
	
	public MemberVO() {}

	public MemberVO(String id, String password, String email, int grade) {
		super();
		this.id = id;
		this.password = password;
		this.email = email;
		this.grade = grade;
	}


	
}