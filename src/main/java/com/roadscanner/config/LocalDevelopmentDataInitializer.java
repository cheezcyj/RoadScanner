package com.roadscanner.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.roadscanner.cmn.validation.CredentialPolicy;

/** Creates generic local-only accounts when an ephemeral password is supplied. */
@Component
@Profile("local")
public class LocalDevelopmentDataInitializer implements InitializingBean {

    private static final Logger LOG = LogManager.getLogger(LocalDevelopmentDataInitializer.class);

    private final JdbcTemplate jdbcTemplate;
    private final String localPassword;

    public LocalDevelopmentDataInitializer(
            JdbcTemplate jdbcTemplate,
            @Value("${ROADSCANNER_LOCAL_PASSWORD:}") String localPassword) {
        this.jdbcTemplate = jdbcTemplate;
        this.localPassword = localPassword;
    }

    @Override
    public void afterPropertiesSet() {
        if (!CredentialPolicy.isValidPassword(localPassword)) {
            LOG.warn("Local accounts were not created; provide a policy-compliant local password");
            return;
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        insertAccount("localuser", "local-user@example.invalid", 1,
                encoder.encode(localPassword));
        insertAccount("localadmin", "local-admin@example.invalid", 2,
                encoder.encode(localPassword));
    }

    private void insertAccount(String id, String email, int grade, String encodedPassword) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM MEMBER WHERE id = ?", Integer.class, id);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO MEMBER (id, password, email, grade) VALUES (?, ?, ?, ?)",
                id, encodedPassword, email, grade);
    }
}
