package com.roadscanner.dao.user;

import com.roadscanner.domain.user.MemberVO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/dao-test-context.xml")
@Transactional
public class MemberDaoTest {

    private static final String RAW_PASSWORD = "password01!";

    @Autowired
    private UserDao dao;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private MemberVO member;

    @Before
    public void setUp() {
        member = new MemberVO("DaoTest01", RAW_PASSWORD, "dao-test01@roadscanner.test", 1);
		jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    public void insertsAndSelectsMemberWithEncodedPassword() throws Exception {
        assertThat(dao.insertOne(member)).isEqualTo(1);

        MemberVO selected = dao.selectOne(byId(member.getId()));

        assertThat(selected).isNotNull();
        assertThat(selected.getId()).isEqualTo(member.getId());
        assertThat(selected.getEmail()).isEqualTo(member.getEmail());
        assertThat(selected.getGrade()).isEqualTo(1);
        assertThat(selected.getNo()).isPositive();
        assertThat(selected.getRegdate()).isNotBlank();
        assertThat(passwordEncoder.matches(RAW_PASSWORD, selected.getPassword())).isTrue();
    }

    @Test
    public void checksIdEmailAndPassword() throws Exception {
        dao.insertOne(member);

        MemberVO credentials = new MemberVO(member.getId(), RAW_PASSWORD, member.getEmail(), 1);
        assertThat(dao.idCheck(credentials)).isEqualTo(1);
        assertThat(dao.emailCheck(credentials)).isEqualTo(1);
        assertThat(dao.passCheck(credentials)).isEqualTo(1);
        assertThat(credentials.getGrade()).isEqualTo(1);
        assertThat(credentials.getCredentialVersion()).isZero();

        credentials.setPassword("wrong-password");
        assertThat(dao.passCheck(credentials)).isZero();

        MemberVO missing = new MemberVO("missing-member", RAW_PASSWORD, null, 1);
        assertThat(dao.passCheck(missing)).isZero();
    }

    @Test
    public void rejectsPasswordBeyondBcryptByteLimit() throws Exception {
        String password72Bytes = repeat("a", 72);
        MemberVO longPasswordMember = new MemberVO(
                "bcryptlimit", password72Bytes, "bcrypt-limit@roadscanner.test", 1);
        dao.insertOne(longPasswordMember);

        assertThat(dao.passCheck(new MemberVO(
                longPasswordMember.getId(), password72Bytes, null, 0))).isEqualTo(1);
        assertThat(dao.passCheck(new MemberVO(
                longPasswordMember.getId(), password72Bytes + "x", null, 0))).isZero();
    }

    @Test
    public void searchesMemberByEmailAndId() throws Exception {
        dao.insertOne(member);
        MemberVO search = new MemberVO(member.getId(), null, member.getEmail(), 0);

        assertThat(dao.searchIdCheck(search)).isEqualTo(1);
        assertThat(dao.searchPwCheck(search)).isEqualTo(1);
        assertThat(dao.searchId(search).getId()).isEqualTo(member.getId());
        assertThat(dao.searchPw(search).getPassword()).isNotBlank();
        assertThat(dao.searchgrade(search).getGrade()).isEqualTo(1);
        assertThat(dao.findIdGrade(search).getGrade()).isEqualTo(1);
        assertThat(dao.findPwGrade(search).getGrade()).isEqualTo(1);
    }

    private String repeat(String value, int count) {
        StringBuilder repeated = new StringBuilder(value.length() * count);
        for (int index = 0; index < count; index++) {
            repeated.append(value);
        }
        return repeated.toString();
    }

    @Test
    public void updatesPasswordAndRejectsSamePassword() throws Exception {
        dao.insertOne(member);
        long originalVersion = dao.selectOne(byId(member.getId())).getCredentialVersion();
        MemberVO change = new MemberVO(member.getId(), "new-password01!", member.getEmail(), 1);

        assertThat(dao.updatePw(change)).isEqualTo(1);
        MemberVO changed = dao.selectOne(byId(member.getId()));
        assertThat(passwordEncoder.matches("new-password01!", changed.getPassword())).isTrue();
        assertThat(changed.getCredentialVersion()).isEqualTo(originalVersion + 1);

		MemberVO staleChange = new MemberVO(
				member.getId(), "another-password01!", member.getEmail(), 1);
		staleChange.setCredentialVersion(originalVersion);
		assertThat(dao.updatePw(staleChange)).isZero();

        MemberVO samePassword = new MemberVO(member.getId(), "new-password01!", member.getEmail(), 1);
        assertThat(dao.updatePw(samePassword)).isEqualTo(3);
    }

    @Test
    public void resetsPasswordByEmail() throws Exception {
        dao.insertOne(member);
        long originalVersion = dao.selectOne(byId(member.getId())).getCredentialVersion();
        MemberVO reset = new MemberVO(null, "reset-password01!", member.getEmail(), 0);

        assertThat(dao.changePw(reset)).isEqualTo(1);
        MemberVO changed = dao.selectOne(byId(member.getId()));
        assertThat(passwordEncoder.matches("reset-password01!", changed.getPassword())).isTrue();
        assertThat(changed.getCredentialVersion()).isEqualTo(originalVersion + 1);
    }

    @Test
    public void suspendsAndRestoresMember() throws Exception {
        dao.insertOne(member);
        MemberVO key = byId(member.getId());

        assertThat(dao.forbiddenGrade(key)).isEqualTo(1);
        assertThat(dao.selectOne(key).getGrade()).isEqualTo(3);

        assertThat(dao.clearGrade(key)).isEqualTo(1);
        assertThat(dao.selectOne(key).getGrade()).isEqualTo(1);
    }

	@Test
	public void locksAndListsActiveAdministratorsForLastAdminGuard() throws Exception {
		assertThat(dao.lockActiveAdministratorIds()).containsExactly("admin");
	}

    @Test
    public void retiresMemberWithoutBreakingAuthoredDataForeignKeys() throws Exception {
        dao.insertOne(member);
        MemberVO key = byId(member.getId());
		jdbcTemplate.update(
				"INSERT INTO UPLOAD_IMAGE "
						+ "(idx, id, category, name, url, file_size, checked, u1, u2, u3) "
						+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
				901, member.getId(), 10, "retained-upload.png",
				"https://example.test/retained-upload.png", 1.0, 0, 0, 0, 0);

        assertThat(dao.deleteOne(key)).isEqualTo(1);
		MemberVO retired = dao.selectOne(key);
		assertThat(retired).isNotNull();
		assertThat(retired.getGrade()).isEqualTo(4);
		assertThat(retired.getEmail()).startsWith("withdrawn-");
		assertThat(dao.passCheck(new MemberVO(
				member.getId(), RAW_PASSWORD, null, 0))).isZero();
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM UPLOAD_IMAGE WHERE id = ?",
				Integer.class,
				member.getId())).isEqualTo(1);
    }

	@Test
	public void staleCredentialVersionCannotWithdrawMember() throws Exception {
		dao.insertOne(member);
		MemberVO reset = new MemberVO(null, "reset-password01!", member.getEmail(), 0);
		assertThat(dao.changePw(reset)).isEqualTo(1);

		MemberVO stale = byId(member.getId());
		stale.setCredentialVersion(0);
		assertThat(dao.withdraw(stale)).isZero();
		assertThat(dao.selectOne(stale).getGrade()).isEqualTo(1);
	}

    private MemberVO byId(String id) {
        MemberVO key = new MemberVO();
        key.setId(id);
        return key;
    }
}
