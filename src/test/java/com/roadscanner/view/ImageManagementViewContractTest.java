package com.roadscanner.view;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ImageManagementViewContractTest {

    @Test
    public void imageManagementContentUsesAViewportFillingMainRegion() throws IOException {
        String jsp = read("src/main/webapp/WEB-INF/views/imgManagement.jsp");
        String css = read("src/main/webapp/resources/css/imgMng.css");

        assertTrue(jsp.contains("<body class=\"rs-internal-page img-management-page\">"));
        assertTrue(jsp.contains("<main class=\"img-management-main\">"));
        assertTrue(jsp.indexOf("<main class=\"img-management-main\">")
                < jsp.indexOf("<div class=\"container img-management-toolbar\">"));
        assertTrue(jsp.indexOf("</main>") < jsp.indexOf("id=\"overlay-modal\""));
        assertTrue(css.contains(".img-management-page {"));
        assertTrue(css.contains("min-height: 100dvh;"));
        assertTrue(css.contains("flex-direction: column;"));
        assertTrue(css.contains(".img-management-main {"));
        assertTrue(css.contains("flex: 1 0 auto;"));
        assertTrue(css.contains(".img-management-page > footer {"));
    }

    @Test
    public void imageActionButtonsDoNotKeepMouseFocusStyling() throws IOException {
        String css = read("src/main/webapp/resources/css/imgMng.css");

        assertTrue(css.contains(".img-bulk-actions .btn:focus:not(:focus-visible):not(:active)"));
        assertTrue(css.contains(".img-detail-actions .btn:focus:not(:focus-visible):not(:active)"));
        assertTrue(css.contains("background-color: var(--rs-primary);"));
        assertTrue(css.contains("box-shadow: none;"));
        assertTrue(css.contains("transform: none;"));
    }

    @Test
    public void administratorDropdownClickMatchesHoverState() throws IOException {
        String css = read("src/main/webapp/resources/css/common.css");

        assertTrue(css.contains("--bs-dropdown-link-hover-color: var(--rs-primary-dark);"));
        assertTrue(css.contains("--bs-dropdown-link-hover-bg: rgba(15, 118, 110, 0.09);"));
        assertTrue(css.contains("--bs-dropdown-link-active-color: var(--rs-primary-dark);"));
        assertTrue(css.contains("--bs-dropdown-link-active-bg: rgba(15, 118, 110, 0.09);"));
        assertTrue(css.contains(".rs-navbar .dropdown-item:focus-visible"));
        assertTrue(css.contains(".rs-navbar .dropdown-item:active"));
        assertTrue(css.contains("background-color: var(--bs-dropdown-link-hover-bg);"));
        assertTrue(css.contains("color: var(--bs-dropdown-link-hover-color);"));
        assertFalse(css.contains("background-color: var(--bs-dropdown-link-active-bg);"));
    }

    @Test
    public void uploadCheckboxIdsRemainUniqueWithinThePage() throws IOException {
        String jsp = read("src/main/webapp/WEB-INF/views/imgManagement.jsp");
        String mapper = read("src/main/resources/mapper/upload/upload.xml");

        assertTrue(jsp.contains("id=\"upload-${vo.idx}-${status.index}\""));
        assertTrue(jsp.contains("for=\"upload-${vo.idx}-${status.index}\""));
        assertTrue(mapper.contains("TT1.idx"));
    }

    private String read(String relativePath) throws IOException {
        Path path = Paths.get(System.getProperty("user.dir")).resolve(relativePath);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
    }
}
