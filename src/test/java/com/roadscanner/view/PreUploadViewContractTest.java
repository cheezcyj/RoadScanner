package com.roadscanner.view;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

public class PreUploadViewContractTest {

    @Test
    public void initialUploadCardKeepsBalancedVerticalSpacing() throws IOException {
        String jsp = read("src/main/webapp/WEB-INF/views/preUpload.jsp");
        String css = read("src/main/webapp/resources/css/preUpload.css");

        assertTrue(jsp.contains("/resources/css/preUpload.css?v=9"));
        assertTrue(jsp.contains("id=\"uploadStatus\" class=\"upload-status\""
                + " role=\"status\" aria-live=\"polite\"></p>"));
        assertMatches(css,
                "\\.upload-card\\s*\\{[^}]*padding:\\s*clamp\\(20px, 4vw, 34px\\);");
        assertMatches(css,
                "\\.upload-dropzone\\s*\\{[^}]*align-content:\\s*center;"
                        + "[^}]*padding:\\s*34px 24px;");
        assertMatches(css,
                "\\.upload-status:empty\\s*\\{[^}]*display:\\s*none;");
        assertMatches(css,
                "@media \\(max-width: 640px\\)[^{]*\\{.*?"
                        + "\\.upload-dropzone\\s*\\{[^}]*padding:\\s*28px 18px;");
    }

    @Test
    public void uploadContentAndRunButtonKeepIntentionalVerticalSpacing() throws IOException {
        String jsp = read("src/main/webapp/WEB-INF/views/preUpload.jsp");
        String css = read("src/main/webapp/resources/css/preUpload.css");

        assertMatches(css,
                "\\.upload-shell\\s*\\{[^}]*display:\\s*grid;"
                        + "[^}]*align-content:\\s*center;"
                        + "[^}]*padding-block:\\s*clamp\\(24px, 4vh, 44px\\);");
        assertMatches(css,
                "\\.run-container\\s*\\{[^}]*margin-top:\\s*16px;");
        assertMatches(css,
                "\\.run-container\\[hidden\\]\\s*\\{[^}]*display:\\s*none !important;");
        assertTrue(jsp.indexOf("id=\"uploadStatus\"")
                < jsp.indexOf("id=\"RunContainer\""));
    }

    private String read(String relativePath) throws IOException {
        Path path = Paths.get(System.getProperty("user.dir")).resolve(relativePath);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private void assertMatches(String value, String regex) {
        assertTrue("Expected pattern was not found: " + regex,
                Pattern.compile(regex, Pattern.DOTALL).matcher(value).find());
    }
}
