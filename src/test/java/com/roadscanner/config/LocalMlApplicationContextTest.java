package com.roadscanner.config;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.roadscanner.service.upload.FileUploadService;
import com.roadscanner.service.upload.LocalFileUploadService;
import com.roadscanner.service.upload.RestTemplateService;
import com.roadscanner.service.upload.RestTemplateServiceImpl;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration("src/main/webapp")
@ActiveProfiles({"local", "local-ml"})
@TestPropertySource(properties = {
        "ROADSCANNER_LOCAL_PASSWORD=LocalMlContext9!",
        "ROADSCANNER_FLASK_API_URL=http://127.0.0.1:5000/predict",
        "roadscanner.storage.delete-retry-delay-ms=3600000"
})
@ContextHierarchy({
        @ContextConfiguration(name = "root",
                locations = "file:src/main/webapp/WEB-INF/root-context.xml"),
        @ContextConfiguration(name = "servlet",
                locations = "file:src/main/webapp/WEB-INF/servlet-context.xml")
})
public class LocalMlApplicationContextTest {

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private RestTemplateService restTemplateService;

    @Autowired
    private TrafficSignResultCatalogValidator resultCatalogValidator;

    @Test
    public void localMlProfileKeepsLocalStorageAndEnablesRealAnalysisClient() {
        assertTrue(fileUploadService instanceof LocalFileUploadService);
        assertTrue(restTemplateService instanceof RestTemplateServiceImpl);
        assertTrue(resultCatalogValidator != null);
    }
}
