package com.roadscanner.dao.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.roadscanner.domain.user.MemberVO;
import com.roadscanner.service.user.UserService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/dao-test-context.xml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AdminMutationConcurrencyTest {

    @Autowired
    private UserService userService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update(
                "INSERT INTO MEMBER (no, id, password, email, grade) VALUES (?, ?, ?, ?, ?)",
                90, "admin2", "test-only-hash", "admin2@example.test", 2);
    }

    @Test(timeout = 15000)
    public void concurrentDeleteAndSuspensionLeaveOneActiveAdministrator() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Integer> suspendAdmin2 = executor.submit(() -> {
                ready.countDown();
                start.await();
                return userService.forbiddenGrade(byId("admin2"));
            });
            Future<Integer> deleteAdmin = executor.submit(() -> {
                ready.countDown();
                start.await();
                return userService.delete(byId("admin"));
            });

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Integer> outcomes = Arrays.asList(
                    suspendAdmin2.get(10, TimeUnit.SECONDS),
                    deleteAdmin.get(10, TimeUnit.SECONDS));

            assertThat(outcomes.stream().filter(outcome -> outcome == 1).count()).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM MEMBER WHERE grade = 2",
                    Integer.class)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private MemberVO byId(String id) {
        MemberVO member = new MemberVO();
        member.setId(id);
        return member;
    }
}
