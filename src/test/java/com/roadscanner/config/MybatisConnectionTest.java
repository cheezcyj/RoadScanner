package com.roadscanner.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"file:src/main/webapp/WEB-INF/root-context.xml", "file:src/main/resources/mybatis-config.xml"})
public class MybatisConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Test
    public void 마이바티스_연결확인() {
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection);
        } catch (SQLException e) {
            fail("Database connection failed: " + e.getMessage());
        }
    }
}
