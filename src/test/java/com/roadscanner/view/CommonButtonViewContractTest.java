package com.roadscanner.view;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CommonButtonViewContractTest {

    @Test
    public void filledGreenButtonsKeepGreenBootstrapStateVariables() throws IOException {
        String css = read("src/main/webapp/resources/css/common.css");
        String block = blockStartingAt(css, ".btn-primary,\n.btn-secondary,\n.btn-warning {");

        assertTrue(block.contains("--bs-btn-bg: var(--rs-primary);"));
        assertTrue(block.contains("--bs-btn-hover-bg: var(--rs-primary-dark);"));
        assertTrue(block.contains("--bs-btn-active-bg: var(--rs-primary-dark);"));
        assertTrue(block.contains("--bs-btn-focus-shadow-rgb: 15, 118, 110;"));
        assertTrue(block.contains("--bs-btn-disabled-bg: var(--rs-primary);"));
        assertTrue(block.contains("--bs-btn-active-border-color: var(--rs-primary-dark);"));
        assertTrue(block.contains("--bs-btn-disabled-border-color: var(--rs-primary);"));
    }

    @Test
    public void outlinedGreenButtonsDoNotReturnToBootstrapBlueOrGray() throws IOException {
        String css = read("src/main/webapp/resources/css/common.css");
        String block = blockStartingAt(css, ".btn-outline-primary,\n.btn-outline-secondary {");

        assertTrue(block.contains("--bs-btn-color: var(--rs-primary);"));
        assertTrue(block.contains("--bs-btn-hover-bg: var(--rs-primary);"));
        assertTrue(block.contains("--bs-btn-active-bg: var(--rs-primary);"));
        assertTrue(block.contains("--bs-btn-focus-shadow-rgb: 15, 118, 110;"));
        assertTrue(block.contains("--bs-btn-disabled-color: var(--rs-primary);"));
    }

    @Test
    public void qnaSaveAndUpdateButtonsUseTheSharedGreenToken() throws IOException {
        String css = read("src/main/webapp/resources/css/qna.css");
        String save = read("src/main/webapp/WEB-INF/views/qna/question-save.jsp");
        String update = read("src/main/webapp/WEB-INF/views/qna/question-update.jsp");

        assertTrue(save.contains("id=\"btn-save\" class=\"btn btn-primary\""));
        assertTrue(update.contains("id=\"btn-update\" class=\"btn btn-primary\""));
        assertFalse(css.contains("#btn-save {"));
        assertFalse(css.contains("#btn-update {"));
    }

    private String blockStartingAt(String css, String selector) {
        int start = css.indexOf(selector);
        assertTrue("Expected selector was not found: " + selector, start >= 0);
        int end = css.indexOf('}', start);
        assertTrue("Expected block terminator was not found: " + selector, end > start);
        return css.substring(start, end + 1);
    }

    private String read(String relativePath) throws IOException {
        Path path = Paths.get(System.getProperty("user.dir")).resolve(relativePath);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
    }
}
