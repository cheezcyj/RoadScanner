package com.roadscanner.view;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UploadViewContractTest {

    @Test
    public void dislikeReasonCardOpensInACenteredFlexContainer() throws IOException {
        String css = read("src/main/webapp/resources/css/upload.css");
        String script = read("src/main/webapp/resources/js/upload/upload.js");

        assertMatches(css,
                "#reasonForm\\s*\\{[^}]*display:\\s*flex;"
                        + "[^}]*width:\\s*100%;"
                        + "[^}]*justify-content:\\s*center;");
        assertTrue(script.contains("reasonForm.style.display = shouldOpen ? 'flex' : 'none';"));
        assertFalse(script.contains("reasonForm.style.display === 'none' ? 'block' : 'none'"));
        assertFalse(script.contains("dislikeReason.style.display"));
    }

    @Test
    public void dislikeButtonExposesTheFeedbackPanelState() throws IOException {
        String jsp = read("src/main/webapp/WEB-INF/views/upload.jsp");
        String script = read("src/main/webapp/resources/js/upload/upload.js");

        assertTrue(jsp.contains("aria-controls=\"reasonForm\" aria-expanded=\"false\""));
        assertTrue(jsp.contains("/resources/js/upload/upload.js?v=9"));
        assertTrue(script.contains("dislikeButton.setAttribute('aria-expanded', String(shouldOpen));"));
    }

    @Test
    public void closingTheResultReturnsToTheInitialAnalysisPage() throws IOException {
        String jsp = read("src/main/webapp/WEB-INF/views/upload.jsp");
        String script = read("src/main/webapp/resources/js/upload/upload.js");
        String css = read("src/main/webapp/resources/css/upload.css");

        assertTrue(jsp.contains("<a id=\"cancelButton\" class=\"btn btn-link\" href=\"${CP}/main/preUpload\""));
        assertTrue(jsp.contains("aria-label=\"사진 분석 초기 화면으로 돌아가기\" title=\"새 이미지 분석\""));
        assertTrue(jsp.contains("<img alt=\"\" aria-hidden=\"true\" src=\"${CP}/resources/img/cancel.png\">"));
        assertFalse(jsp.contains("<button id=\"cancelButton\""));
        assertFalse(jsp.contains("onclick="));
        assertFalse(jsp.contains("id=\"fileUpload\""));
        assertFalse(jsp.contains("result-upload-dropzone"));
        assertFalse(jsp.contains("RunContainer"));
        assertFalse(jsp.contains("analysisUploadStatus"));
        assertFalse(jsp.contains("class=\"divider\""));
        assertFalse(script.contains("displaySelectedFile"));
        assertFalse(script.contains("FileReader"));
        assertFalse(script.contains("/main/fileUploaded"));
        assertFalse(script.contains("cancelButton.addEventListener"));
        assertFalse(script.contains("analysisViewRevision"));
        assertFalse(script.contains("isCurrentAnalysisView"));
        assertFalse(css.contains(".result-upload-"));
        assertFalse(css.contains(".is-upload-only"));
        assertFalse(css.contains("#runButton"));
        assertFalse(css.contains("input[type=\"file\"]"));
    }

    @Test
    public void resultPageKeepsThePreviewAndAnalysisColumns() throws IOException {
        String jsp = read("src/main/webapp/WEB-INF/views/upload.jsp");
        String css = read("src/main/webapp/resources/css/upload.css");

        assertTrue(jsp.contains("/resources/css/upload.css?v=7"));
        assertTrue(jsp.contains("/resources/js/upload/upload.js?v=9"));
        assertMatches(css,
                "#separation\\s*\\{[^}]*display:\\s*grid;"
                        + "[^}]*grid-template-columns:\\s*repeat\\(2, minmax\\(0, 1fr\\)\\);"
                        + "[^}]*flex:\\s*1 0 auto;"
                        + "[^}]*align-content:\\s*center;"
                        + "[^}]*1180px");
        assertMatches(css,
                "#cancelButton\\s*\\{[^}]*position:\\s*absolute;"
                        + "[^}]*display:\\s*grid;"
                        + "[^}]*border-radius:\\s*50%;");
    }

    @Test
    public void resultCardsAreCenteredBetweenTheHeaderAndFooter() throws IOException {
        String jsp = read("src/main/webapp/WEB-INF/views/upload.jsp");
        String css = read("src/main/webapp/resources/css/upload.css");

        assertTrue(jsp.contains("<body class=\"rs-internal-page d-flex flex-column min-vh-100\">"));
        assertTrue(jsp.indexOf("/WEB-INF/views/layout/navbar.jsp")
                < jsp.indexOf("<main id=\"separation\""));
        assertTrue(jsp.indexOf("<main id=\"separation\"")
                < jsp.indexOf("/WEB-INF/views/layout/footer.jsp"));
        assertMatches(css,
                "#separation\\s*\\{[^}]*margin:\\s*0 auto;"
                        + "[^}]*padding-block:\\s*clamp\\(12px, 2vw, 24px\\);");
    }

    @Test
    public void feedbackSubmissionKeepsTheCurrentResultVisible() throws IOException {
        String jsp = read("src/main/webapp/WEB-INF/views/upload.jsp");
        String script = read("src/main/webapp/resources/js/upload/upload.js");

        assertTrue(jsp.contains("id=\"likeButton\" type=\"button\""));
        assertTrue(jsp.contains("id=\"dislikeButton\" type=\"button\""));
        assertTrue(jsp.contains("id=\"submitButton\" type=\"button\""));
        assertTrue(script.contains("function keepSubmittedResultVisible()"));
        assertTrue(jsp.contains("id=\"feedbackSubmitted\""));
        assertTrue(script.contains("$(\"#feedbackSubmitted\").val() === \"true\""));
        assertTrue(script.contains(".prop(\"disabled\", true)"));
        assertTrue(script.contains("keepSubmittedResultVisible();"));
        assertFalse(script.contains("window.location.href = contextPath + \"/main/preUpload\""));
        assertFalse(script.contains("window.location.reload"));
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
