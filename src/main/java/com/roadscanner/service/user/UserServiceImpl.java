package com.roadscanner.service.user;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.roadscanner.dao.user.UserDao;
import com.roadscanner.cmn.validation.CredentialPolicy;
import com.roadscanner.domain.user.MemberVO;

@Service("userService")
public class UserServiceImpl implements UserService {
	private static final int NORMAL_USER_GRADE = 1;
	private static final int ADMIN_GRADE = 2;
	private static final int SUCCESS = 10;
	private static final int FAILURE = 20;
	private static final String NOT_FOUND = "-1";
	private static final String BANNED_USER = "2";
	private static final String PASSWORD_RESET_ALLOWED = "1";

	final Logger LOG = LogManager.getLogger(getClass());

	private final UserDao userDao;

	@Autowired
	public UserServiceImpl(UserDao userDao) {
		this.userDao = userDao;
	}
	
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
		
		if (!CredentialPolicy.isValidUserId(user.getId())
				|| !CredentialPolicy.isValidPassword(user.getPassword())) {
			return FAILURE;
		}

		int idCheck = this.userDao.idCheck(user);
		int emailCheck = this.userDao.emailCheck(user);
		
		LOG.debug("MembershipServiceImpl idCheck : "+idCheck);
		LOG.debug("MembershipServiceImpl emailCheck : "+emailCheck);
		
		LOG.debug("└────────────────────────────────────────────────────────┘");
		
		// 10: 가입 성공 / 20: 가입 실패
		if (idCheck != 0 || emailCheck != 0) {
			return FAILURE;
		}

		// Never trust a client-provided grade during self-registration.
		user.setGrade(NORMAL_USER_GRADE);
		int inserted = this.userDao.insertOne(user);
		LOG.debug("MembershipServiceImpl inserted : "+inserted);

		return inserted == 1 ? SUCCESS : FAILURE;
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
		int accountExists = this.userDao.idCheck(user);
		// Always invoke passCheck. The DAO uses a dummy BCrypt hash when the account is absent.
		int passwordMatches = this.userDao.passCheck(user);
		int checkStatus;
		if (accountExists != 1) {
			checkStatus = 10;
		} else if (passwordMatches != 1) {
			checkStatus = 20;
		} else {
			// passCheck copied grade and credentialVersion from the same row whose
			// password hash was verified. Do not replace that snapshot with a later read.
			if (!isActiveGrade(user.getGrade())) {
				checkStatus = 40;
			} else {
				checkStatus = 30;
			}
		}

		LOG.debug("│ login result code recorded");
		LOG.debug("└────────────────────────────────────────────────────────┘");		
		return checkStatus;
	}

	@Override
	public String doSearchId(MemberVO user) throws SQLException {
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ UserServiceImpl doSearchId()                           │");
		LOG.debug("└────────────────────────────────────────────────────────┘");
        String result = NOT_FOUND;
        
		int flag = this.userDao.searchIdCheck(user);
		if (flag != 1) {
			return NOT_FOUND;
		}

		MemberVO gradeInfo = this.userDao.findIdGrade(user);
		if (gradeInfo == null) {
			return NOT_FOUND;
		}

		if (!isActiveGrade(gradeInfo.getGrade())) {
			return BANNED_USER;
		}

		MemberVO foundUser = this.userDao.searchId(user);
		if (foundUser != null && foundUser.getId() != null) {
			result = foundUser.getId();
		}
		
       
        return result;
	}
	
	@Override
    public String doSearchPw(MemberVO user) throws SQLException {//10(id 없음)/20(비밀번호 수정 오류),30(비밀번호 수정 성공) 
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ UserServiceImpl doSearchPw()                           │");
		LOG.debug("└────────────────────────────────────────────────────────┘");
		String pwresult = NOT_FOUND;
		
		int checkStatus = this.userDao.searchPwCheck(user);
		if (checkStatus != 1) {
			return NOT_FOUND;
		}

		MemberVO gradeInfo = this.userDao.findPwGrade(user);
		if (gradeInfo == null) {
			return NOT_FOUND;
		}

		if (!isActiveGrade(gradeInfo.getGrade())) {
			return BANNED_USER;
		}

		// A successful lookup is represented by a status only; never return a password hash.
		pwresult = PASSWORD_RESET_ALLOWED;
        
        
        return pwresult;
    }

	@Override
	@Transactional
	public int doChangeInfo(MemberVO user) throws SQLException {
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ UserServiceImpl doChangeInfo()                         │");
		LOG.debug("└────────────────────────────────────────────────────────┘");
		int checkStatus = -1;
		if (!CredentialPolicy.isValidPassword(user.getPassword())) {
			return checkStatus;
		}
		if (user.getCurrentPassword() == null || user.getCurrentPassword().isEmpty()) {
			return 2;
		}
		MemberVO currentCredentials = new MemberVO();
		currentCredentials.setId(user.getId());
		currentCredentials.setPassword(user.getCurrentPassword());
		if (this.userDao.passCheck(currentCredentials) != 1) {
			return 2;
		}
		user.setCredentialVersion(currentCredentials.getCredentialVersion());
       
        checkStatus = this.userDao.updatePw(user);
    
        LOG.debug("checkStatus: " + checkStatus);
        return checkStatus;
	}
	
	@Override
	@Transactional
	public int doWithdraw(MemberVO user) {
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ UserServiceImpl doWithdraw()                           │");
		LOG.debug("└────────────────────────────────────────────────────────┘");
	    int checkStatus = 0;
	    try {
			MemberVO currentUser = this.userDao.selectOne(user);
			if (currentUser == null || currentUser.getGrade() != NORMAL_USER_GRADE) {
				return checkStatus;
			}
	        int flag = this.userDao.passCheck(user);
	             
	        if(flag == 1 && user.getGrade() == NORMAL_USER_GRADE) {
	        	 checkStatus = this.userDao.withdraw(user);
	        	 LOG.debug(checkStatus);
	        }else {
	    	    return checkStatus;
	        }
	        
	    } catch (SQLException | DataAccessException e) {
	        LOG.error("withdraw_persistence_failed");
	    }
	    return checkStatus;
	}
	
	@Override
	@Transactional
	public int delete(MemberVO user) throws SQLException {
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ UserServiceImpl delete()                           │");
		LOG.debug("└────────────────────────────────────────────────────────┘");
	    try {
			MemberVO target = this.userDao.selectOne(user);
			if (target == null || !isActiveGrade(target.getGrade())) {
				return 0;
			}
			if (target.getGrade() == ADMIN_GRADE) {
				java.util.List<String> activeAdministratorIds =
						this.userDao.lockActiveAdministratorIds();
				if (!activeAdministratorIds.contains(target.getId())
						|| activeAdministratorIds.size() <= 1) {
					return 0;
				}
			}
			user.setCredentialVersion(target.getCredentialVersion());
			return this.userDao.deleteOne(user);
		} catch (DataAccessException exception) {
			LOG.error("account_retirement_failed");
			return 0;
		}
	}

	private boolean isActiveGrade(int grade) {
		return grade == NORMAL_USER_GRADE || grade == ADMIN_GRADE;
	}

	@Override
	@Transactional
	public int forbiddenGrade(MemberVO user) throws SQLException {
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ UserServiceImpl forbiddenGrade()                       │");
		
		int checkGrade = -1;
		MemberVO target = this.userDao.selectOne(user);
		if (target == null || !isActiveGrade(target.getGrade())) {
			return checkGrade;
		}
		if (target.getGrade() == ADMIN_GRADE) {
			java.util.List<String> activeAdministratorIds =
					this.userDao.lockActiveAdministratorIds();
			if (!activeAdministratorIds.contains(target.getId())
					|| activeAdministratorIds.size() <= 1) {
				return checkGrade;
			}
		}
		user.setCredentialVersion(target.getCredentialVersion());
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
		if (!CredentialPolicy.isValidPassword(user.getPassword())) {
			return checkStatus;
		}
		
        checkStatus = this.userDao.changePw(user);
        if(0 == checkStatus) {
            checkStatus = -1; // 회원정보가 변경되지 않음
        } 
        LOG.debug("checkStatus: " + checkStatus);
        LOG.debug("└────────────────────────────────────────────────────────┘");
        return checkStatus;
	}
	
}
