package com.pcwk.ehr.user.domain;

import com.pcwk.ehr.cmn.DTO;

public class UserVO extends DTO {
	private String userId  ;//회원id
	private String name    ;//이름
	private String passwd  ;//비번
	
	/*2023.05.26 추가 */
	private Level  level   ;//BASIC,SILVER,GOLD
	private int login;     //로그인
	private int recommend; //추천수
	private String email;  //이메일
	private String reg_dt; //등록일
	
	private int intLevel ;//mybatis level -> int
	
	
	public UserVO() {}

	public UserVO(String userId, String name, String passwd, Level level, int login, int recommend, String email,
			String reg_dt) {
		super();
		this.userId = userId;
		this.name = name;
		this.passwd = passwd;
		this.level = level;
		this.login = login;
		this.recommend = recommend;
		this.email = email;
		this.reg_dt = reg_dt;
		
		this.intLevel = level.intValue();
	}
	
	public int getIntLevel() {
		return intLevel;
	}

	public void setIntLevel(int intLevel) {
		this.intLevel = intLevel;
		
		//mybatis  -> DAO
		this.level = Level.valueOf(intLevel);
	}

	/**
	 * 현제 Level이 다음 레벨로 변경!
	 */
	public void upgradeLevel() {
		Level nextLevel = this.level.nextLevel();
		
		if(null == nextLevel) {
			throw new IllegalArgumentException(this.level+"은 업그레이드가 불가능합니다.");
		}else {
			this.level = nextLevel;
		}
	}
	


	public Level getLevel() {
		return level;
	}

	public void setLevel(Level level) {
		this.level = level;
		
		//intLevel
		if(null !=level) {
			this.intLevel = level.intValue();
		}
	}

	public int getLogin() {
		return login;
	}

	public void setLogin(int login) {
		this.login = login;
	}

	public int getRecommend() {
		return recommend;
	}

	public void setRecommend(int recommend) {
		this.recommend = recommend;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getReg_dt() {
		return reg_dt;
	}

	public void setReg_dt(String reg_dt) {
		this.reg_dt = reg_dt;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPasswd() {
		return passwd;
	}

	public void setPasswd(String passwd) {
		this.passwd = passwd;
	}

	@Override
	public String toString() {
		return "UserVO [userId=" + userId + ", name=" + name + ", passwd=" + passwd + ", level=" + level + ", login="
				+ login + ", recommend=" + recommend + ", email=" + email + ", reg_dt=" + reg_dt + ", intLevel="
				+ intLevel + ", toString()=" + super.toString() + "]";
	}




	
	
	

}
