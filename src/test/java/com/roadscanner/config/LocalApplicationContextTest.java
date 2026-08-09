package com.roadscanner.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.roadscanner.service.upload.FileUploadService;
import com.roadscanner.service.upload.LocalFileUploadService;
import com.roadscanner.service.upload.LocalRestTemplateService;
import com.roadscanner.service.upload.RestTemplateService;
import com.roadscanner.service.user.LocalMailSendService;
import com.roadscanner.service.user.MailSendService;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration("src/main/webapp")
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "ROADSCANNER_LOCAL_PASSWORD=LocalContext9!",
        "roadscanner.storage.delete-retry-delay-ms=3600000"
})
@ContextHierarchy({
        @ContextConfiguration(name = "root",
                locations = "file:src/main/webapp/WEB-INF/root-context.xml"),
        @ContextConfiguration(name = "servlet",
                locations = "file:src/main/webapp/WEB-INF/servlet-context.xml")
})
public class LocalApplicationContextTest {

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private RestTemplateService restTemplateService;

    @Autowired
    private MailSendService mailSendService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void localProfileLoadsOnlyIsolatedAdaptersAndGenericAccounts() {
        assertTrue(fileUploadService instanceof LocalFileUploadService);
        assertTrue(restTemplateService instanceof LocalRestTemplateService);
        assertTrue(mailSendService instanceof LocalMailSendService);

        Integer accountCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM MEMBER WHERE id IN ('localuser', 'localadmin')",
                Integer.class);
        assertEquals(Integer.valueOf(2), accountCount);

        Integer resultCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM RESULT_IMAGE WHERE no BETWEEN 1 AND 44",
                Integer.class);
        assertEquals(Integer.valueOf(44), resultCount);
        assertEquals("Turn right ahead", jdbcTemplate.queryForObject(
                "SELECT name FROM RESULT_IMAGE WHERE no = 34", String.class));
    }
}
