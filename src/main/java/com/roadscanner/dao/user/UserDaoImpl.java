package com.roadscanner.dao.user;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import com.roadscanner.cmn.validation.CredentialPolicy;
import com.roadscanner.domain.user.MemberVO;

@Repository("userDao")
public class UserDaoImpl implements UserDao {
	private static final String DUMMY_PASSWORD_HASH =
			"$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
	final String NAMESPACE = "com.roadscanner.dao.user.UserDao";
	final String DOT = ".";
	PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	
	@Autowired
	SqlSessionTemplate sqlSessionTemplate;

	private final static Logger LOG = LogManager.getLogger(UserDaoImpl.class);

	// default 생성
	public UserDaoImpl() {}
	
	
	@Override
	public MemberVO selectOne(MemberVO inVO) throws SQLException {
		String statement = this.NAMESPACE + ".selectOne";
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ statement : " + statement);
		LOG.debug("└────────────────────────────────────────────────────────┘");
		MemberVO outVO = this.sqlSessionTemplate.selectOne(statement, inVO);

		if (outVO == null) {
			LOG.debug("쿼리 결과가 없습니다.");
		}
		return outVO;
	}

	@Override
	public int idCheck(MemberVO user) throws SQLException {
		int flag = 0;
		String statement = this.NAMESPACE + DOT + "idCheck";
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ 1. statement : " + statement);
		flag = this.sqlSessionTemplate.selectOne(statement, user);
		LOG.debug("│ 3. flag : " + flag);
		LOG.debug("└────────────────────────────────────────────────────────┘");
		return flag;
	}
	
	@Override
	public int emailCheck(MemberVO user) throws SQLException {
		int flag = 0;
		String statement = this.NAMESPACE + DOT + "emailCheck";
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ 1. statement : " + statement);
		LOG.debug("└────────────────────────────────────────────────────────┘");
		flag = this.sqlSessionTemplate.selectOne(statement, user);

		return flag;
	}
	
	@Override
	public int passCheck(MemberVO user) throws SQLException {
		String statement = this.NAMESPACE + DOT + "passCheck";
		String statementpw = this.NAMESPACE + DOT + "encoder";
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ 1. statement " + statement);
		MemberVO storedCredential = this.sqlSessionTemplate.selectOne(statementpw, user);
		LOG.debug("└────────────────────────────────────────────────────────┘");

		String submittedPassword = user.getPassword() == null ? "" : user.getPassword();
		boolean hasStoredHash = storedCredential != null
				&& storedCredential.getPassword() != null
				&& !storedCredential.getPassword().trim().isEmpty();
		String storedHash = hasStoredHash ? storedCredential.getPassword() : DUMMY_PASSWORD_HASH;
		boolean matches;
		try {
			// A missing account still performs the same deliberately expensive BCrypt comparison.
			matches = matchesPassword(submittedPassword, storedHash);
		} catch (IllegalArgumentException exception) {
			// Corrupt legacy hashes must fail closed without restoring a fast failure path.
			matchesPassword(submittedPassword, DUMMY_PASSWORD_HASH);
			matches = false;
		}
		if (!hasStoredHash || !matches) {
			return 0;
		}

		// Bind the authorization snapshot to the password comparison. Callers can
		// reject a later read/update if the account changed after this BCrypt check.
		user.setGrade(storedCredential.getGrade());
		user.setCredentialVersion(storedCredential.getCredentialVersion());
		return 1;
	}

	@Override
	public int insertOne(MemberVO user) throws SQLException {
		String encoder = encodePassword(user.getPassword());
		user.setPassword(encoder);
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ MembershipDaoImpl addUser()");
		LOG.debug("└────────────────────────────────────────────────────────┘");
		
		
		int flag = 0;
		String statement = this.NAMESPACE + DOT + "insertOne";
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ 1. statement " + statement);
		LOG.debug("└────────────────────────────────────────────────────────┘");
		
		flag = this.sqlSessionTemplate.insert(statement, user);

		return flag;
	}

	@Override
	public int deleteOne(MemberVO user) throws SQLException {
		int flag = 0;
		String statement = this.NAMESPACE + DOT + "deleteOne";
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ statement : " + statement);
		LOG.debug("└────────────────────────────────────────────────────────┘");
		user.setPassword(encodePassword(UUID.randomUUID().toString()));
		flag = this.sqlSessionTemplate.update(statement, user);

		return flag;
	}

	@Override
	public List<String> lockActiveAdministratorIds() throws SQLException {
		return this.sqlSessionTemplate.selectList(
				this.NAMESPACE + DOT + "lockActiveAdministratorIds");
	}

	@Override
	public MemberVO searchId(MemberVO user) throws SQLException {
		String statement = this.NAMESPACE + DOT + "searchId";
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ statement : " + statement);
		LOG.debug("└────────────────────────────────────────────────────────┘");
		MemberVO outVO = this.sqlSessionTemplate.selectOne(statement, user);

		return outVO;
	}

	@Override
	public int searchIdCheck(MemberVO user) throws SQLException {
		int flag = 0;
		String statement = this.NAMESPACE + DOT + "searchIdCheck";
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ statement : " + statement);
		LOG.debug("└────────────────────────────────────────────────────────┘");
		flag = this.sqlSessionTemplate.selectOne(statement, user);

		return flag;
	}

	@Override
	public int searchPwCheck(MemberVO user) throws SQLException {	
		int flag = 0;
		String statement = this.NAMESPACE + DOT + "searchPwCheck";
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ statement : " + statement);
		LOG.debug("└────────────────────────────────────────────────────────┘");
		flag = this.sqlSessionTemplate.selectOne(statement, user);

		return flag;
	}
	
	@Override
	public MemberVO searchPw(MemberVO user) throws SQLException {
		String statement = this.NAMESPACE + DOT + "searchPw";
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ statement : " + statement);
		LOG.debug("└────────────────────────────────────────────────────────┘");
		MemberVO outVO = this.sqlSessionTemplate.selectOne(statement, user);

		return outVO;
	}
	
	@Override
	public MemberVO searchgrade(MemberVO user) throws SQLException {
		String statement = this.NAMESPACE + DOT + "searchgrade";
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ statement : " + statement);
		LOG.debug("└────────────────────────────────────────────────────────┘");
		MemberVO outVO = this.sqlSessionTemplate.selectOne(statement, user);

		return outVO;
	}

	@Override
	public int updatePw(MemberVO user) throws SQLException {
		String statementpw = this.NAMESPACE + DOT + "encoder";
		MemberVO encoderpw = this.sqlSessionTemplate.selectOne(statementpw,user);

		String statement = this.NAMESPACE + DOT + "updatepassword";
		int flag = -1;
		
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ 1. statement : " + statement);
		LOG.debug("└────────────────────────────────────────────────────────┘");

		if (encoderpw == null) {
			return flag;
		}

		if(matchesPassword(user.getPassword(),encoderpw.getPassword())) {
			LOG.debug("┌────────────────────────────────────────────────────────┐");
			LOG.debug("│ UserDaoImpl updatePw() error");
			LOG.debug("└────────────────────────────────────────────────────────┘");
			flag = 3;
			return flag;
			
		} else {
			LOG.debug("┌────────────────────────────────────────────────────────┐");
			LOG.debug("│ UserDaoImpl updatePw() success");

			String encoder = encodePassword(user.getPassword());
			user.setPassword(encoder);

			LOG.debug("└────────────────────────────────────────────────────────┘");
			
			flag = this.sqlSessionTemplate.update(statement, user);
		}
			return flag;
	}

	@Override
	public int withdraw(MemberVO user) throws SQLException {
	    int flag = 0;
	    String statement = this.NAMESPACE + DOT + "withdrawOne";
	    LOG.debug("┌────────────────────────────────────────────────────────┐");
	    LOG.debug("│ statement : " + statement);
	    LOG.debug("└────────────────────────────────────────────────────────┘");
	    user.setPassword(encodePassword(UUID.randomUUID().toString()));
	    flag = this.sqlSessionTemplate.update(statement, user);

	    return flag;
	}

	@Override
	public int forbiddenGrade(MemberVO user) throws SQLException {
		int flag = 0;
		String statement = this.NAMESPACE + DOT + "forbiddenUser";
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ statement : " + statement);
		LOG.debug("└────────────────────────────────────────────────────────┘");
		flag = this.sqlSessionTemplate.update(statement, user);

		return flag;
	}

	@Override
	public int clearGrade(MemberVO user) throws SQLException {
		int flag = 0;
		String statement = this.NAMESPACE + DOT + "clearUser";
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ statement : " + statement);
		LOG.debug("└────────────────────────────────────────────────────────┘");
		flag = this.sqlSessionTemplate.update(statement, user);

		return flag;
	}

	@Override
	public int changePw(MemberVO user) throws SQLException {
		
		String encoder = encodePassword(user.getPassword());
		user.setPassword(encoder);
		
		int flag = 0;
		String statement = this.NAMESPACE + DOT + "changePw";
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ statement : " + statement);
		LOG.debug("└────────────────────────────────────────────────────────┘");
		flag = this.sqlSessionTemplate.update(statement, user);

		return flag;
	}

	@Override
	public MemberVO findIdGrade(MemberVO user) throws SQLException {
		String statement = this.NAMESPACE + DOT + "findIdGrade";
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ statement : " + statement);
		LOG.debug("└────────────────────────────────────────────────────────┘");
		MemberVO outVO = this.sqlSessionTemplate.selectOne(statement, user);

		return outVO;
	}


	@Override
	public MemberVO findPwGrade(MemberVO user) throws SQLException {
		String statement = this.NAMESPACE + DOT + "findPwGrade";
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ statement : " + statement);
		LOG.debug("└────────────────────────────────────────────────────────┘");
		MemberVO outVO = this.sqlSessionTemplate.selectOne(statement, user);

		return outVO;
	}

	private boolean matchesPassword(String rawPassword, String encodedPassword) {
		return CredentialPolicy.isBcryptLengthValid(rawPassword)
				&& passwordEncoder.matches(rawPassword, encodedPassword);
	}

	private String encodePassword(String rawPassword) {
		if (!CredentialPolicy.isBcryptLengthValid(rawPassword)) {
			throw new IllegalArgumentException("Password exceeds BCrypt's 72-byte limit.");
		}
		return passwordEncoder.encode(rawPassword);
	}
	
}
