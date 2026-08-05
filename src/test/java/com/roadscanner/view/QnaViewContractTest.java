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

public class QnaViewContractTest {

    @Test
    public void inquiryAndBoardListActionsRemainDistinct() throws IOException {
        String jsp = read("src/main/webapp/WEB-INF/views/qna/index.jsp");

        assertTrue(jsp.contains("class=\"qna-title-link\">내 문의글 보기</a>"));
        assertMatches(jsp,
                "<c:when\\s+test=\"\\$\\{resolvedViewMode eq 'myInquiry'\\}\">\\s*"
                        + "<div class=\"d-flex justify-content-end qna-list-actions\">\\s*"
                        + "<a href=\"\\$\\{CP\\}/qna/inquiry/save\"[^>]*qna-write-button[^>]*>문의글쓰기</a>");
        assertMatches(jsp,
                "<c:when\\s+test=\"\\$\\{resolvedViewMode eq 'board'\\}\">\\s*"
                        + "<div class=\"d-flex justify-content-end qna-list-actions\">\\s*"
                        + "<a href=\"\\$\\{CP\\}/qna/save\"[^>]*>Q&amp;A 글쓰기</a>");
    }

    @Test
    public void listWriteActionsKeepTopSpacing() throws IOException {
        String css = read("src/main/webapp/resources/css/qna.css");

        assertMatches(css, "\\.qna-list-actions\\s*\\{[^}]*margin-top:\\s*20px;");
    }

    @Test
    public void writeButtonKeepsGreenHoverFocusAndActiveStates() throws IOException {
        String css = read("src/main/webapp/resources/css/qna.css");

        assertTrue(css.contains(".qna-write-button:focus,"));
        assertTrue(css.contains(".qna-write-button:active,"));
        assertTrue(css.contains("--bs-btn-active-bg: var(--rs-primary);"));
        assertTrue(css.contains("background: var(--rs-primary);"));
    }

    @Test
    public void listFiltersDoNotAddAFocusOrClickRing() throws IOException {
        String css = read("src/main/webapp/resources/css/qna.css");

        assertMatches(css,
                "#category:focus,\\s*#category:active,\\s*"
                        + "#searchType:focus,\\s*#searchType:active\\s*\\{"
                        + "[^}]*border-color:\\s*#ced4da;"
                        + "[^}]*outline:\\s*none;"
                        + "[^}]*box-shadow:\\s*none;");
        assertFalse(css.contains("#category:focus,\n#searchType:focus,\n#keyword:focus"));
    }

    @Test
    public void listHeaderDoesNotRenderAnExtraTopDivider() throws IOException {
        String jsp = read("src/main/webapp/WEB-INF/views/qna/index.jsp");

        assertTrue(jsp.contains("<thead>"));
        assertFalse(jsp.contains("<thead class=\"table-group-divider\">"));
        assertTrue(jsp.contains("<tbody class=\"table-group-divider\">"));
    }

    @Test
    public void listPaginationKeepsSpaceAboveTheControls() throws IOException {
        String jsp = read("src/main/webapp/WEB-INF/views/qna/index.jsp");
        String css = read("src/main/webapp/resources/css/qna.css");

        assertTrue(jsp.contains("<nav class=\"qna-pagination\" aria-label=\"페이지 이동\">"));
        assertMatches(css, "\\.qna-pagination\\s*\\{[^}]*margin:\\s*24px 0 8px;");
    }

    @Test
    public void answeredInquiryKeepsSpaceBelowTheWrittenAnswerCard() throws IOException {
        String jsp = read("src/main/webapp/WEB-INF/views/qna/question-detail.jsp");

        assertTrue(jsp.contains("<div class=\"row mx-2 mb-3\">"));
        assertTrue(jsp.contains("class=\"card-body py-2\""));
        assertFalse(jsp.contains("<div class=\"row mx-2 mb-1\">"));
        assertFalse(jsp.contains("qna-answer-content-body"));
    }

    @Test
    public void cancellingAnswerUpdateRestoresTheDetailViewAndOriginalContent() throws IOException {
        String jsp = read("src/main/webapp/WEB-INF/views/qna/question-detail.jsp");
        String script = read("src/main/webapp/resources/js/qna.js");

        assertTrue(jsp.contains("id=\"btn-answer-cancel-update\""));
        assertTrue(jsp.contains("/resources/js/qna.js?v=5"));
        assertTrue(script.contains("$('#btn-answer-cancel-update').on('click'"));
        assertTrue(script.contains("$('#answer-update-content').val(originalUpdateContent);"));
        assertTrue(script.contains("$('#answer-detail').css('display', 'block');"));
    }

    @Test
    public void editorLimitsVisibleTextInsteadOfSerializedHtml() throws IOException {
        String save = read("src/main/webapp/WEB-INF/views/qna/question-save.jsp");
        String editor = read("src/main/webapp/WEB-INF/views/qna/editor.jsp");
        String script = read("src/main/webapp/resources/js/qna-editor.js");

        assertTrue(save.contains("/resources/js/qna-editor.js?v=3"));
        assertTrue(editor.contains("data-maxlength=\"10000\""));
        assertFalse(editor.contains(" maxlength=\"10000\""));
        assertTrue(script.contains("surface.textContent || \"\""));
        assertTrue(script.contains("Array.from(visibleText).length"));
        assertTrue(script.contains("source.removeAttribute(\"maxlength\")"));

        String submitScript = read("src/main/webapp/resources/js/qna.js");
        assertTrue(submitScript.contains("if (!this.validateQuestionForm())"));
        assertTrue(submitScript.contains("content.validationMessage"));
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
