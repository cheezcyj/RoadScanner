package com.roadscanner.service.user;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.roadscanner.dao.user.UserDao;
import com.roadscanner.domain.user.MemberVO;

@Service("userService")
public class UserServiceImpl implements UserService {
	final Logger LOG = LogManager.getLogger(getClass());
	
	@Autowired
	private UserDao userDao;
	
	@Override
	public MemberVO selectUser(MemberVO user) throws SQLException {
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ UserServiceImpl selectUser()                           │");
		LOG.debug("└────────────────────────────────────────────────────────┘");
		return userDao.selectOne(user);
	}
	
	@Override
	public int register(MemberVO user) throws SQLException {
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ UserServiceImpl register()                             │");		
		
		int idCheck = this.userDao.idCheck(user);
		int emailCheck = this.userDao.emailCheck(user);
		int flag = this.userDao.insertOne(user);
		
		LOG.debug("MembershipServiceImpl idCheck : "+idCheck);
		LOG.debug("MembershipServiceImpl emailCheck : "+emailCheck);
		LOG.debug("MembershipServiceImpl flag : "+flag);
		
		LOG.debug("└────────────────────────────────────────────────────────┘");
		
		// 10: 가입 성공 / 20: 가입 실패
		if(1 != idCheck || 1 != emailCheck) {
			flag = 10;
		} else {
			flag = 20;
		}
		
		return flag;
	}
		
	@Override
	public int doIdDuplCheck(MemberVO user) throws SQLException {
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ UserServiceImpl doIdDuplCheck()                        │");
		LOG.debug("└────────────────────────────────────────────────────────┘");
		
		int result = 0;
		int flag = 0;
		
		flag = this.userDao.idCheck(user);
		
		// 10: 중복 존재, 20: 중복 없음
		if(1 == flag) {
			result = 10;
		} else if (0 == flag) {
			result = 20;
		} 
		return result;
	}
	
	@Override
	public int doEmailDuplCheck(MemberVO user) throws SQLException {
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ UserServiceImpl doEmailDuplCheck()                     │");
		LOG.debug("└────────────────────────────────────────────────────────┘");
		
		int result = 0;
		int flag = 0;
		
		flag = this.userDao.emailCheck(user);
		
		// 10: 중복 존재, 20: 중복 없음
		if(1 == flag) {
			result = 10;
		} else if (0 == flag) {
			result = 20;
		} 
		return result;
	}

	@Override
	public int doLogin(MemberVO user) throws SQLException {
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ UserServiceImpl doLogin()                              │");
		
		
		// 10: id 없음, 20: 비밀번호 오류, 30: 성공, 40: 정지된 회원
		int checkStatus = 0; 		
		int status = this.userDao.idCheck(user);
		
		if(1 == status) {
			LOG.debug("│ UserServiceImpl doLogin success                    │");
			String grade = String.valueOf(this.userDao.searchgrade(user).getGrade());
			status = userDao.passCheck(user);
			
				if(1 == status){
					
					if(grade.equals("3")) {
						checkStatus = 40; 
						return checkStatus;
					}
					
					checkStatus = 30;
					
				}if(0 == status){
					checkStatus = 20;
					
				}
				
		}else {
			checkStatus = 10;

		}
	
		LOG.debug("│ checkStatus : "+ checkStatus);
		LOG.debug("└────────────────────────────────────────────────────────┘");		
		return checkStatus;
	}

	@Override
	public String doSearchId(MemberVO user) throws SQLException {
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ UserServiceImpl doSearchId()                           │");
		LOG.debug("└────────────────────────────────────────────────────────┘");
        String result = "-1";
        
		int flag =0;
		flag = this.userDao.searchIdCheck(user);
		
        if(0==flag) {
            result = "-1";
        }else if(1==flag) {
            result = this.userDao.searchId(user).getId(); 
        } 
        return result;
	}
	
	@Override
    public String doSearchPw(MemberVO user) throws SQLException {//10(id 없음)/20(비밀번호 수정 오류),30(비밀번호 수정 성공) 
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ UserServiceImpl doSearchPw()                           │");
		LOG.debug("└────────────────────────────────────────────────────────┘");
		String pwresult = "-1";
		
		int checkStatus = 0;
        checkStatus = this.userDao.searchPwCheck(user);
        
        if(0==checkStatus) {
        	pwresult = "-1";
        } else if(1==checkStatus) {
        	pwresult = this.userDao.searchPw(user).getPassword(); 
        } 
        return pwresult;
    }

	@Override
	public int doChangeInfo(MemberVO user) throws SQLException {
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ UserServiceImpl doChangeInfo()                         │");
		LOG.debug("└────────────────────────────────────────────────────────┘");
		int checkStatus = -1;
       
        checkStatus = this.userDao.updatePw(user);
    
        LOG.debug("checkStatus: " + checkStatus);
        return checkStatus;
	}
	
	@Override
	public int doWithdraw(MemberVO user) {
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ UserServiceImpl doWithdraw()                           │");
		LOG.debug("└────────────────────────────────────────────────────────┘");
	    int checkStatus = 0;
	    try {
	        int flag = this.userDao.passCheck(user);      
	             
	        if(flag == 1) {
	        	 checkStatus = this.userDao.withdraw(user);
	        	 LOG.debug(checkStatus);
	        }else {
	    	    return checkStatus;
	        }
	        
	    } catch (SQLException e) {
	        LOG.error("Error occurred while withdrawing user: " + e.getMessage());
	    }
	    return checkStatus;
	}
	
	@Override
	public int delete(MemberVO user) throws SQLException {
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ UserServiceImpl delete()                           │");
		LOG.debug("└────────────────────────────────────────────────────────┘");
	    int checkStatus = 0;
	    checkStatus = this.userDao.deleteOne(user);
	    return checkStatus;
	}

	@Override
	public int forbiddenGrade(MemberVO user) throws SQLException {
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ UserServiceImpl forbiddenGrade()                       │");
		
		int checkGrade = -1;
		
        checkGrade = this.userDao.forbiddenGrade(user);
        if(0 == checkGrade) {
            checkGrade = -1; // 회원정보가 변경되지 않음
        } 
        LOG.debug("checkGrade: " + checkGrade);
        LOG.debug("└────────────────────────────────────────────────────────┘");
        return checkGrade;
	}

	@Override
	public int clearGrade(MemberVO user) throws SQLException {
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ UserServiceImpl clearGrade()                           │");
		
		int checkGrade = -1;
		
        checkGrade = this.userDao.clearGrade(user);
        if(0 == checkGrade) {
            checkGrade = -1; // 회원정보가 변경되지 않음
        } 
        LOG.debug("checkGrade: " + checkGrade);
        LOG.debug("└────────────────────────────────────────────────────────┘");
        return checkGrade;
	}

	@Override
	public int changePw(MemberVO user) throws SQLException {
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ UserServiceImpl changePw()                             │");
		
		int checkStatus = -1;
		
        checkStatus = this.userDao.changePw(user);
        if(0 == checkStatus) {
            checkStatus = -1; // 회원정보가 변경되지 않음
        } 
        LOG.debug("checkStatus: " + checkStatus);
        LOG.debug("└────────────────────────────────────────────────────────┘");
        return checkStatus;
	}
	
}
