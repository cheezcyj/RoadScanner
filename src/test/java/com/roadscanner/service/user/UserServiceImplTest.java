package com.roadscanner.service.user;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import com.roadscanner.dao.user.UserDao;
import com.roadscanner.domain.user.MemberVO;

public class UserServiceImplTest {

    private UserDao userDao;
    private UserServiceImpl userService;

    @Before
    public void setUp() {
        userDao = mock(UserDao.class);
        userService = new UserServiceImpl(userDao);
    }

    @Test
    public void registerInsertsAvailableAccountAsNormalUser() throws SQLException {
        MemberVO user = member("newuser1", "new@example.com", 2);
        when(userDao.idCheck(user)).thenReturn(0);
        when(userDao.emailCheck(user)).thenReturn(0);
        when(userDao.insertOne(user)).thenReturn(1);

        int result = userService.register(user);

        assertEquals(10, result);
        assertEquals(1, user.getGrade());
        InOrder calls = inOrder(userDao);
        calls.verify(userDao).idCheck(user);
        calls.verify(userDao).emailCheck(user);
        calls.verify(userDao).insertOne(user);
    }

    @Test
    public void registerDoesNotInsertDuplicateId() throws SQLException {
        MemberVO user = member("duplicate", "new@example.com", 2);
        when(userDao.idCheck(user)).thenReturn(1);
        when(userDao.emailCheck(user)).thenReturn(0);

        assertEquals(20, userService.register(user));

        verify(userDao, never()).insertOne(any(MemberVO.class));
    }

    @Test
    public void registerDoesNotInsertDuplicateEmail() throws SQLException {
        MemberVO user = member("newuser1", "duplicate@example.com", 2);
        when(userDao.idCheck(user)).thenReturn(0);
        when(userDao.emailCheck(user)).thenReturn(1);

        assertEquals(20, userService.register(user));

        verify(userDao, never()).insertOne(any(MemberVO.class));
    }

    @Test
    public void registerReportsFailureWhenInsertAffectsNoRow() throws SQLException {
        MemberVO user = member("newuser1", "new@example.com", 2);
        when(userDao.idCheck(user)).thenReturn(0);
        when(userDao.emailCheck(user)).thenReturn(0);
        when(userDao.insertOne(user)).thenReturn(0);

        assertEquals(20, userService.register(user));
        assertEquals(1, user.getGrade());
    }

    @Test
    public void searchIdReturnsNotFoundWithoutLookingUpGrade() throws SQLException {
        MemberVO query = member(null, "missing@example.com", 0);
        when(userDao.searchIdCheck(query)).thenReturn(0);

        assertEquals("-1", userService.doSearchId(query));

        verify(userDao, never()).findIdGrade(any(MemberVO.class));
        verify(userDao, never()).searchId(any(MemberVO.class));
    }

    @Test
    public void searchIdHandlesMissingGradeRow() throws SQLException {
        MemberVO query = member(null, "orphaned@example.com", 0);
        when(userDao.searchIdCheck(query)).thenReturn(1);
        when(userDao.findIdGrade(query)).thenReturn(null);

        assertEquals("-1", userService.doSearchId(query));

        verify(userDao, never()).searchId(any(MemberVO.class));
    }

    @Test
    public void searchIdReturnsBannedStatusWithoutReturningId() throws SQLException {
        MemberVO query = member(null, "banned@example.com", 0);
        when(userDao.searchIdCheck(query)).thenReturn(1);
        when(userDao.findIdGrade(query)).thenReturn(member("banned", "banned@example.com", 3));

        assertEquals("2", userService.doSearchId(query));

        verify(userDao, never()).searchId(any(MemberVO.class));
    }

    @Test
    public void searchIdReturnsActiveUserId() throws SQLException {
        MemberVO query = member(null, "active@example.com", 0);
        when(userDao.searchIdCheck(query)).thenReturn(1);
        when(userDao.findIdGrade(query)).thenReturn(member("active", "active@example.com", 1));
        when(userDao.searchId(query)).thenReturn(member("active", "active@example.com", 1));

        assertEquals("active", userService.doSearchId(query));
    }

    @Test
    public void searchPasswordReturnsNotFoundWithoutLookingUpGrade() throws SQLException {
        MemberVO query = member("missing", "missing@example.com", 0);
        when(userDao.searchPwCheck(query)).thenReturn(0);

        assertEquals("-1", userService.doSearchPw(query));

        verify(userDao, never()).findPwGrade(any(MemberVO.class));
        verify(userDao, never()).searchPw(any(MemberVO.class));
    }

    @Test
    public void searchPasswordHandlesMissingGradeRow() throws SQLException {
        MemberVO query = member("orphaned", "orphaned@example.com", 0);
        when(userDao.searchPwCheck(query)).thenReturn(1);
        when(userDao.findPwGrade(query)).thenReturn(null);

        assertEquals("-1", userService.doSearchPw(query));

        verify(userDao, never()).searchPw(any(MemberVO.class));
    }

    @Test
    public void searchPasswordReturnsBannedStatusWithoutReadingHash() throws SQLException {
        MemberVO query = member("banned", "banned@example.com", 0);
        when(userDao.searchPwCheck(query)).thenReturn(1);
        when(userDao.findPwGrade(query)).thenReturn(member("banned", "banned@example.com", 3));

        assertEquals("2", userService.doSearchPw(query));

        verify(userDao, never()).searchPw(any(MemberVO.class));
    }

    @Test
    public void searchPasswordReturnsStatusWithoutReadingHash() throws SQLException {
        MemberVO query = member("active", "active@example.com", 0);
        when(userDao.searchPwCheck(query)).thenReturn(1);
        when(userDao.findPwGrade(query)).thenReturn(member("active", "active@example.com", 1));

        assertEquals("1", userService.doSearchPw(query));

        verify(userDao, never()).searchPw(any(MemberVO.class));
    }

    @Test
    public void registerRejectsInvalidCredentialsBeforeDatabaseLookup() throws SQLException {
        MemberVO user = new MemberVO("bad", "weak", "new@example.com", 1);

        assertEquals(20, userService.register(user));

        verify(userDao, never()).idCheck(any(MemberVO.class));
        verify(userDao, never()).insertOne(any(MemberVO.class));
    }

    @Test
    public void missingAccountStillUsesPasswordVerificationPath() throws SQLException {
        MemberVO credentials = member("missing", null, 0);
        when(userDao.idCheck(credentials)).thenReturn(0);
        when(userDao.passCheck(credentials)).thenReturn(0);

        assertEquals(10, userService.doLogin(credentials));

        InOrder calls = inOrder(userDao);
        calls.verify(userDao).idCheck(credentials);
        calls.verify(userDao).passCheck(credentials);
        verify(userDao, never()).searchgrade(any(MemberVO.class));
    }

    @Test
    public void wrongPasswordDoesNotReadAccountGrade() throws SQLException {
        MemberVO credentials = member("member01", null, 0);
        when(userDao.idCheck(credentials)).thenReturn(1);
        when(userDao.passCheck(credentials)).thenReturn(0);

        assertEquals(20, userService.doLogin(credentials));

        verify(userDao, never()).searchgrade(any(MemberVO.class));
    }

    @Test
    public void validPasswordChecksGradeBeforeSuccessfulLogin() throws SQLException {
        MemberVO credentials = member("member01", null, 0);
        when(userDao.idCheck(credentials)).thenReturn(1);
        when(userDao.passCheck(credentials)).thenAnswer(invocation -> {
            credentials.setGrade(1);
            credentials.setCredentialVersion(7);
            return 1;
        });

        assertEquals(30, userService.doLogin(credentials));
        assertEquals(7, credentials.getCredentialVersion());
        verify(userDao, never()).searchgrade(any(MemberVO.class));
    }

    @Test
    public void inactivePasswordSnapshotFailsClosed() throws SQLException {
        MemberVO credentials = member("member01", null, 0);
        when(userDao.idCheck(credentials)).thenReturn(1);
        when(userDao.passCheck(credentials)).thenAnswer(invocation -> {
            credentials.setGrade(3);
            credentials.setCredentialVersion(4);
            return 1;
        });

        assertEquals(40, userService.doLogin(credentials));
        verify(userDao, never()).searchgrade(any(MemberVO.class));
    }

    @Test
    public void passwordChangeRequiresTheCurrentPassword() throws SQLException {
        MemberVO change = member("member01", "member@example.com", 1);
        change.setPassword("NewPassword1!");
        change.setCurrentPassword("WrongPassword1!");
        when(userDao.passCheck(any(MemberVO.class))).thenReturn(0);

        assertEquals(2, userService.doChangeInfo(change));

        verify(userDao, never()).updatePw(change);
    }

    @Test
    public void verifiedCurrentPasswordAllowsPasswordChange() throws SQLException {
        MemberVO change = member("member01", "member@example.com", 1);
        change.setPassword("NewPassword1!");
        change.setCurrentPassword("CurrentPassword1!");
        when(userDao.passCheck(any(MemberVO.class))).thenAnswer(invocation -> {
            MemberVO verified = invocation.getArgument(0);
            verified.setCredentialVersion(9);
            verified.setGrade(1);
            return 1;
        });
        when(userDao.updatePw(change)).thenReturn(1);

        assertEquals(1, userService.doChangeInfo(change));

        assertEquals(9, change.getCredentialVersion());
        verify(userDao).updatePw(change);
    }

	@Test
	public void selfWithdrawalUsesPasswordSnapshotVersionAndGrade() throws SQLException {
		MemberVO request = member("member01", "member@example.com", 1);
		when(userDao.selectOne(request)).thenReturn(request);
		when(userDao.passCheck(request)).thenAnswer(invocation -> {
			request.setGrade(1);
			request.setCredentialVersion(12);
			return 1;
		});
		when(userDao.withdraw(request)).thenReturn(1);

		assertEquals(1, userService.doWithdraw(request));
		assertEquals(12, request.getCredentialVersion());
	}

	@Test
	public void selfWithdrawalRejectsAccountPromotedDuringPasswordCheck() throws SQLException {
		MemberVO request = member("member01", "member@example.com", 1);
		when(userDao.selectOne(request)).thenReturn(request);
		when(userDao.passCheck(request)).thenAnswer(invocation -> {
			request.setGrade(2);
			request.setCredentialVersion(13);
			return 1;
		});

		assertEquals(0, userService.doWithdraw(request));
		verify(userDao, never()).withdraw(any(MemberVO.class));
	}

    @Test
    public void administratorCannotUseOrdinaryWithdrawal() throws SQLException {
        MemberVO request = member("admin", "admin@example.com", 2);
        when(userDao.selectOne(request)).thenReturn(request);

        assertEquals(0, userService.doWithdraw(request));

        verify(userDao, never()).withdraw(any(MemberVO.class));
    }

    @Test
    public void lastAdministratorCannotBeRetired() throws SQLException {
        MemberVO target = member("admin", "admin@example.com", 2);
        when(userDao.selectOne(target)).thenReturn(target);
        when(userDao.lockActiveAdministratorIds()).thenReturn(Collections.singletonList("admin"));

        assertEquals(0, userService.delete(target));

        verify(userDao, never()).deleteOne(any(MemberVO.class));
    }

    @Test
    public void administratorCanBeRetiredWhenAnotherAdministratorRemains() throws SQLException {
        MemberVO target = member("admin", "admin@example.com", 2);
        when(userDao.selectOne(target)).thenReturn(target);
        when(userDao.lockActiveAdministratorIds()).thenReturn(Arrays.asList("admin", "admin2"));
        when(userDao.deleteOne(target)).thenReturn(1);

        assertEquals(1, userService.delete(target));
    }

	@Test
	public void lastAdministratorCannotBeSuspended() throws SQLException {
		MemberVO target = member("admin", "admin@example.com", 2);
		target.setCredentialVersion(5);
		when(userDao.selectOne(target)).thenReturn(target);
		when(userDao.lockActiveAdministratorIds()).thenReturn(Collections.singletonList("admin"));

		assertEquals(-1, userService.forbiddenGrade(target));
		verify(userDao, never()).forbiddenGrade(any(MemberVO.class));
	}

    private MemberVO member(String id, String email, int grade) {
        return new MemberVO(id, "Password1!", email, grade);
    }
}
