package com.roadscanner.view;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HeaderViewContractTest {

    private static final String HEADER_INCLUDE = "/WEB-INF/views/layout/header.jsp";
    private static final String NAVBAR_INCLUDE = "/WEB-INF/views/layout/navbar.jsp";

    private static final String[] COMPLETE_INTERNAL_PAGES = {
            "src/main/webapp/WEB-INF/views/graph.jsp",
            "src/main/webapp/WEB-INF/views/imgManagement.jsp",
            "src/main/webapp/WEB-INF/views/preUpload.jsp",
            "src/main/webapp/WEB-INF/views/upload.jsp",
            "src/main/webapp/WEB-INF/views/local/mailbox.jsp",
            "src/main/webapp/WEB-INF/views/login/admin.jsp",
            "src/main/webapp/WEB-INF/views/login/changePw.jsp",
            "src/main/webapp/WEB-INF/views/login/findIdAndPw.jsp",
            "src/main/webapp/WEB-INF/views/login/mypage.jsp",
            "src/main/webapp/WEB-INF/views/login/registerpage.jsp",
            "src/main/webapp/WEB-INF/views/login/withdraw.jsp",
            "src/main/webapp/WEB-INF/views/qna/index.jsp",
            "src/main/webapp/WEB-INF/views/qna/question-detail.jsp",
            "src/main/webapp/WEB-INF/views/qna/question-save.jsp",
            "src/main/webapp/WEB-INF/views/qna/question-update.jsp"
    };

    private static final String[][] PAGE_STYLESHEETS = {
            {"src/main/webapp/WEB-INF/views/graph.jsp", "/resources/css/graph.css"},
            {"src/main/webapp/WEB-INF/views/imgManagement.jsp", "/resources/css/imgMng.css"},
            {"src/main/webapp/WEB-INF/views/preUpload.jsp", "/resources/css/preUpload.css"},
            {"src/main/webapp/WEB-INF/views/upload.jsp", "/resources/css/upload.css"},
            {"src/main/webapp/WEB-INF/views/login/admin.jsp", "/resources/css/admin.css"},
            {"src/main/webapp/WEB-INF/views/login/changePw.jsp", "/resources/css/changePw.css"},
            {"src/main/webapp/WEB-INF/views/login/findIdAndPw.jsp", "/resources/css/default.css"},
            {"src/main/webapp/WEB-INF/views/login/list_admin.jsp", "/resources/css/admin.css"},
            {"src/main/webapp/WEB-INF/views/login/list_banned.jsp", "/resources/css/admin.css"},
            {"src/main/webapp/WEB-INF/views/login/list_member.jsp", "/resources/css/admin.css"},
            {"src/main/webapp/WEB-INF/views/login/login.jsp", "/resources/css/default.css"},
            {"src/main/webapp/WEB-INF/views/login/mypage.jsp", "/resources/css/mypage.css"},
            {"src/main/webapp/WEB-INF/views/login/registerpage.jsp", "/resources/css/membership-style.css"},
            {"src/main/webapp/WEB-INF/views/login/withdraw.jsp", "/resources/css/withdraw.css"},
            {"src/main/webapp/WEB-INF/views/qna/index.jsp", "/resources/css/qna.css"},
            {"src/main/webapp/WEB-INF/views/qna/question-detail.jsp", "/resources/css/qna.css"},
            {"src/main/webapp/WEB-INF/views/qna/question-save.jsp", "/resources/css/qna.css"},
            {"src/main/webapp/WEB-INF/views/qna/question-update.jsp", "/resources/css/qna.css"}
    };

    private static final String[] QNA_PAGES = {
            "src/main/webapp/WEB-INF/views/qna/index.jsp",
            "src/main/webapp/WEB-INF/views/qna/question-detail.jsp",
            "src/main/webapp/WEB-INF/views/qna/question-save.jsp",
            "src/main/webapp/WEB-INF/views/qna/question-update.jsp"
    };

    private static final Pattern PAGE_STYLESHEET_VALUE = Pattern.compile(
            "<c:set\\s+var=\"pageStylesheet\"\\s+value=\"([^\"]+)\"\\s*/?>");

    private static final Pattern CSS_BLOCK = Pattern.compile("([^{}]+)\\{[^{}]*}", Pattern.DOTALL);

    private static final Pattern COMMON_HEADER_SELECTOR = Pattern.compile(
            "(?:\\.rs-site-header|\\.rs-navbar|\\.roadscanner|\\.rs-brand-icon"
                    + "|\\.rs-welcome-wrap|\\.rs-nav-actions|\\.rs-header-action|#welcome"
                    + "|\\.navbar|\\.nav-item|\\.nav-link|\\.dropdown-menu|\\.dropdown-item)"
                    + "(?=$|[\\s>+~.:#\\[,\\-{])");

    @Test
    public void completeInternalPagesUseOneSharedHeaderAndNavbar() throws IOException {
        assertEquals("공통 내부 페이지 목록은 의도한 15개여야 합니다.",
                15, COMPLETE_INTERNAL_PAGES.length);

        for (String path : COMPLETE_INTERNAL_PAGES) {
            String jsp = read(path);
            assertEquals(path + " must include the document header once",
                    1, count(jsp, HEADER_INCLUDE));
            assertEquals(path + " must include the shared navbar once",
                    1, count(jsp, NAVBAR_INCLUDE));
            assertTrue(path + " must use the common internal-page body",
                    Pattern.compile("<body\\b[^>]*class=\"[^\"]*\\brs-internal-page\\b[^\"]*\"")
                            .matcher(jsp).find());

            int header = jsp.indexOf(HEADER_INCLUDE);
            int body = jsp.indexOf("<body");
            int navbar = jsp.indexOf(NAVBAR_INCLUDE);
            assertTrue(path + " must include header before body", header < body);
            assertTrue(path + " must include navbar inside body", body < navbar);
            assertFalse(path + " must not carry a second, page-local Bootstrap navbar",
                    jsp.contains("class=\"navbar"));
        }
    }

    @Test
    public void standaloneAndFragmentViewsRemainExplicitHeaderExceptions() throws IOException {
        String login = read("src/main/webapp/WEB-INF/views/login/login.jsp");
        assertEquals(1, count(login, HEADER_INCLUDE));
        assertEquals(0, count(login, NAVBAR_INCLUDE));
        assertTrue(login.contains("<body class=\"auth-page"));
        assertFalse(login.contains("rs-internal-page"));

        String admin = read("src/main/webapp/WEB-INF/views/login/admin.jsp");
        String[] iframePages = {"list_member", "list_admin", "list_banned"};
        for (String page : iframePages) {
            String path = "src/main/webapp/WEB-INF/views/login/" + page + ".jsp";
            String jsp = read(path);
            assertEquals(path + " needs its own document head inside the iframe",
                    1, count(jsp, HEADER_INCLUDE));
            assertEquals(path + " must not nest the site navbar inside the iframe",
                    0, count(jsp, NAVBAR_INCLUDE));
            assertTrue(path + " must retain the iframe body contract",
                    jsp.contains("<body class=\"admin-list-page\""));
            assertFalse(jsp.contains("rs-internal-page"));
            assertTrue("admin.jsp must embed " + page,
                    admin.contains("src=\"${CP}/login/" + page + "\""));
        }

        String editor = read("src/main/webapp/WEB-INF/views/qna/editor.jsp");
        assertEquals(0, count(editor, HEADER_INCLUDE));
        assertEquals(0, count(editor, NAVBAR_INCLUDE));
        assertFalse(editor.contains("<html"));
        assertFalse(editor.contains("<head"));
        assertFalse(editor.contains("<body"));
        assertTrue(editor.contains("data-qna-editor"));

        String main = read("src/main/webapp/WEB-INF/views/login/main.jsp");
        assertEquals(0, count(main, HEADER_INCLUDE));
        assertEquals(0, count(main, NAVBAR_INCLUDE));
        assertTrue(main.contains("<header class=\"site-header\">"));
        assertFalse(main.contains("rs-site-header"));
        assertFalse(main.contains("rs-navbar"));
    }

    @Test
    public void pageSpecificStylesheetsUseTheHeaderSlotBeforeHeaderInclude() throws IOException {
        for (String[] contract : PAGE_STYLESHEETS) {
            String path = contract[0];
            String expectedStylesheet = contract[1];
            String jsp = read(path);
            Matcher matcher = PAGE_STYLESHEET_VALUE.matcher(jsp);

            assertTrue(path + " must set pageStylesheet", matcher.find());
            assertTrue(path + " must select " + expectedStylesheet,
                    matcher.group(1).startsWith(expectedStylesheet));
            assertEquals(path + " must have one pageStylesheet assignment",
                    1, countMatches(PAGE_STYLESHEET_VALUE, jsp));
            assertTrue(path + " must set pageStylesheet before including header.jsp",
                    matcher.start() < jsp.indexOf(HEADER_INCLUDE));
            assertFalse(path + " must not add a direct stylesheet link",
                    Pattern.compile("<link\\b", Pattern.CASE_INSENSITIVE).matcher(jsp).find());
        }

        Path views = resolve("src/main/webapp/WEB-INF/views");
        try (Stream<Path> files = Files.walk(views)) {
            files.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".jsp"))
                    .filter(path -> !path.endsWith("login/main.jsp"))
                    .filter(path -> !path.endsWith("layout/header.jsp"))
                    .forEach(path -> assertNoDirectStylesheetLink(path, views));
        }
    }

    @Test
    public void qnaStylesheetIsLocalToTheFourQnaPages() throws IOException {
        String header = read("src/main/webapp/WEB-INF/views/layout/header.jsp");
        assertFalse("qna.css must not be loaded globally", header.contains("qna.css"));
        assertEquals(1, count(header, "${pageStylesheet}"));

        for (String path : QNA_PAGES) {
            String jsp = read(path);
            Matcher matcher = PAGE_STYLESHEET_VALUE.matcher(jsp);
            assertTrue(path + " must configure qna.css", matcher.find());
            assertTrue(path + " must configure qna.css before header.jsp",
                    matcher.group(1).startsWith("/resources/css/qna.css")
                            && matcher.start() < jsp.indexOf(HEADER_INCLUDE));
        }
    }

    @Test
    public void sharedNavbarOwnsOneWrapperAndStableGeometry() throws IOException {
        String navbar = read("src/main/webapp/WEB-INF/views/layout/navbar.jsp");
        String header = read("src/main/webapp/WEB-INF/views/layout/header.jsp");
        String common = read("src/main/webapp/resources/css/common.css");

        assertEquals(1, count(navbar, "<header class=\"rs-site-header\">"));
        assertEquals(1, count(navbar, "</header>"));
        assertEquals(1, count(navbar, "<nav class=\"navbar navbar-expand-md rs-navbar\""));
        assertTrue(navbar.indexOf("<header class=\"rs-site-header\">")
                < navbar.indexOf("<nav class=\"navbar navbar-expand-md rs-navbar\""));
        assertTrue(navbar.indexOf("</nav>") < navbar.indexOf("</header>"));
        assertTrue(navbar.contains("<div class=\"container-fluid\">"));
        assertFalse(header.contains("rs-site-header"));
        assertFalse(header.contains("rs-navbar"));

        String wrapperRule = compactCss(cssRule(common, ".rs-site-header"));
        assertTrue(wrapperRule.contains("width:100%"));
        assertTrue(wrapperRule.contains("margin:00clamp(20px,3vw,36px)"));
        assertTrue(wrapperRule.contains("flex:00auto"));

        String navbarRule = compactCss(cssRule(common, ".rs-navbar"));
        assertTrue(navbarRule.contains("width:100%"));
        assertTrue(navbarRule.contains("min-height:65px"));
        assertTrue(navbarRule.contains("margin:0"));
        assertTrue(navbarRule.contains("border-radius:0"));
        assertTrue(navbarRule.contains("background:var(--rs-surface)"));

        String containerRule = compactCss(cssRule(common, ".rs-navbar .container-fluid"));
        assertTrue(containerRule.contains("width:100%"));
        assertTrue(containerRule.contains("max-width:1440px"));
        assertTrue(containerRule.contains("min-height:44px"));
        assertTrue(containerRule.contains("margin-inline:auto"));
        assertTrue(containerRule.contains("padding:0"));

        assertTrue(common.contains("overflow-y: scroll;"));
        assertTrue(common.contains("scrollbar-gutter: stable;"));
        assertFalse(common.contains("backdrop-filter: blur"));
    }

    @Test
    public void qnaPagesUseOnlyTheSharedPageHeadingContract() throws IOException {
        for (String path : QNA_PAGES) {
            String jsp = read(path);
            assertTrue(path + " must use rs-page-shell", jsp.contains("class=\"rs-page-shell"));
            assertTrue(path + " must use rs-page-heading", jsp.contains("class=\"rs-page-heading\""));
            assertTrue(path + " must use rs-page-eyebrow", jsp.contains("class=\"rs-page-eyebrow\""));
            assertTrue(path + " must use rs-page-title", jsp.contains("class=\"rs-page-title\""));
            assertFalse(path + " must not restore qna-shell", jsp.contains("qna-shell"));
            assertFalse(path + " must not restore qna-page-heading", jsp.contains("qna-page-heading"));
            assertFalse(path + " must not restore qna-heading", jsp.contains("qna-heading"));
        }
    }

    @Test
    public void pageStylesheetsDoNotOverrideCommonHeaderSelectors() throws IOException {
        Path cssDirectory = resolve("src/main/webapp/resources/css");
        try (Stream<Path> files = Files.walk(cssDirectory)) {
            files.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".css"))
                    .filter(path -> !path.endsWith("common.css"))
                    .filter(path -> !path.getFileName().toString().startsWith("bootstrap"))
                    .forEach(this::assertDoesNotOverrideCommonHeader);
        }
    }

    @Test
    public void headerMarkUsesFaviconPaletteWithoutInnerCircle() throws IOException {
        String navbar = read("src/main/webapp/WEB-INF/views/layout/navbar.jsp");
        String mark = read("src/main/webapp/resources/img/roadscanner-mark.svg");
        String header = read("src/main/webapp/WEB-INF/views/layout/header.jsp");

        assertTrue(navbar.contains("/resources/img/roadscanner-mark.svg"));
        assertTrue(header.contains("/resources/img/favicon.svg"));
        assertTrue(mark.contains("#07111f"));
        assertTrue(mark.contains("#55e6cf"));
        assertTrue(mark.contains("#f3c64f"));
        assertFalse(mark.contains("<circle"));
    }

    private void assertNoDirectStylesheetLink(Path path, Path views) {
        try {
            String jsp = readAbsolute(path);
            assertFalse(views.relativize(path) + " must route styles through pageStylesheet/header.jsp",
                    Pattern.compile("<link\\b", Pattern.CASE_INSENSITIVE).matcher(jsp).find());
        } catch (IOException exception) {
            throw new AssertionError("Could not read " + path, exception);
        }
    }

    private void assertDoesNotOverrideCommonHeader(Path path) {
        try {
            String css = readAbsolute(path).replaceAll("(?s)/\\*.*?\\*/", "");
            Matcher blocks = CSS_BLOCK.matcher(css);
            while (blocks.find()) {
                String selector = blocks.group(1).trim();
                assertFalse(path.getFileName() + " must not override common header selector: " + selector,
                        COMMON_HEADER_SELECTOR.matcher(selector).find());
            }
        } catch (IOException exception) {
            throw new AssertionError("Could not read " + path, exception);
        }
    }

    private String cssRule(String css, String selector) {
        Pattern rule = Pattern.compile(Pattern.quote(selector) + "\\s*\\{([^}]*)}", Pattern.DOTALL);
        Matcher matcher = rule.matcher(css);
        assertTrue("Missing CSS rule for " + selector, matcher.find());
        return matcher.group(1);
    }

    private String compactCss(String css) {
        return css.replaceAll("\\s+", "").toLowerCase();
    }

    private int count(String value, String needle) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private int countMatches(Pattern pattern, String value) {
        int count = 0;
        Matcher matcher = pattern.matcher(value);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private String read(String relativePath) throws IOException {
        return readAbsolute(resolve(relativePath));
    }

    private String readAbsolute(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private Path resolve(String relativePath) {
        return Paths.get(System.getProperty("user.dir")).resolve(relativePath);
    }
}
