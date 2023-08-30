package com.roadscanner.dao.user;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import com.roadscanner.domain.user.MemberVO;

@Repository("userDao")
public class UserDaoImpl implements UserDao {
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

		if (outVO != null) {
			LOG.debug(outVO.toString());
		} else {
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
		LOG.debug("│ 2. param : " + user.toString());		
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
		LOG.debug("│ 2. param : " + user.toString());
		LOG.debug("└────────────────────────────────────────────────────────┘");
		flag = this.sqlSessionTemplate.selectOne(statement, user);

		return flag;
	}
	
	@Override
	public int passCheck(MemberVO user) throws SQLException {
		
		int flag = 0;
		String statement = this.NAMESPACE + DOT + "passCheck";
		String statementpw = this.NAMESPACE + DOT + "encoder";
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ 1. statement " + statement);
		LOG.debug("│ 2. param : " + user.toString());

		MemberVO encoderpw = this.sqlSessionTemplate.selectOne(statementpw, user);	

		LOG.debug("│ 3 . encoderpw :" + encoderpw.getPassword());
		LOG.debug("└────────────────────────────────────────────────────────┘");
		
	if(passwordEncoder.matches(user.getPassword(),encoderpw.getPassword())) {
		flag = 1;
	} else {
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ UserDaoImpt passCheck() error");
		LOG.debug("└────────────────────────────────────────────────────────┘");
		return flag;
	}
		return flag;		
	}

	@Override
	public int insertOne(MemberVO user) throws SQLException {
		String encoder = passwordEncoder.encode(user.getPassword());
		user.setPassword(encoder);
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ MembershipDaoImpl addUser()");
		LOG.debug("│ passwordEncoder : "+ encoder);
		LOG.debug("└────────────────────────────────────────────────────────┘");
		
		
		int flag = 0;
		String statement = this.NAMESPACE + DOT + "insertOne";
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ 1. statement " + statement);
		LOG.debug("│ 2. param : " + user.toString());
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
		flag = this.sqlSessionTemplate.delete(statement, user);

		return flag;
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
		LOG.debug("│ 2. encoder : " + encoderpw);
		LOG.debug("└────────────────────────────────────────────────────────┘");
					
		if(passwordEncoder.matches(user.getPassword(),encoderpw.getPassword())) {
			LOG.debug("┌────────────────────────────────────────────────────────┐");
			LOG.debug("│ UserDaoImpl updatePw() error");
			LOG.debug("└────────────────────────────────────────────────────────┘");
			flag = 3;
			return flag;
			
		} else {
			LOG.debug("┌────────────────────────────────────────────────────────┐");
			LOG.debug("│ UserDaoImpl updatePw() success");

			String encoder = passwordEncoder.encode(user.getPassword());
			user.setPassword(encoder);

			LOG.debug("│ encoder : " + encoder);
			LOG.debug("└────────────────────────────────────────────────────────┘");
			
			flag = this.sqlSessionTemplate.update(statement, user);
		}
			return flag;
	}

	@Override
	public int withdraw(MemberVO user) throws SQLException {
	    int flag = 0;
	    String statement = this.NAMESPACE + DOT + "deleteOne";
	    LOG.debug("┌────────────────────────────────────────────────────────┐");
	    LOG.debug("│ statement : " + statement);
	    LOG.debug("│ param : " + user.toString());
	    LOG.debug("└────────────────────────────────────────────────────────┘");
	    flag = this.sqlSessionTemplate.delete(statement, user);

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
		
		String encoder = passwordEncoder.encode(user.getPassword());
		user.setPassword(encoder);
		
		int flag = 0;
		String statement = this.NAMESPACE + DOT + "changePw";
		LOG.debug("┌────────────────────────────────────────────────────────┐");
		LOG.debug("│ statement : " + statement);
		LOG.debug("│ encoder : " + encoder);
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
	
}