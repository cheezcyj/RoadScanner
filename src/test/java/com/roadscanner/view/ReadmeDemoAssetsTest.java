package com.roadscanner.view;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReadmeDemoAssetsTest {

    private static final Pattern DEMO_IMAGE = Pattern.compile(
            "!\\[[^\\]]*\\]\\((docs/demo/[^)\\s]+\\.gif)\\)");

    private static final Set<String> EXPECTED_FILES = new TreeSet<>(Arrays.asList(
            "account-login.gif",
            "account-recovery.gif",
            "account-registration.gif",
            "admin-account-management.gif",
            "admin-analysis-statistics.gif",
            "admin-image-data-management.gif",
            "admin-qna-management.gif",
            "image-analysis-flow.gif",
            "landing-overview.gif",
            "private-inquiry-list.gif",
            "profile-management.gif",
            "qna-answer-management.gif",
            "qna-notice-crud.gif",
            "qna-post-crud.gif"
    ));

    private static final List<String> LEGACY_FILES = Arrays.asList(
            "answer-crud.gif",
            "board-management.gif",
            "data-statistics.gif",
            "image-data-management.gif",
            "login.gif",
            "main-page.gif",
            "member-management.gif",
            "my-page.gif",
            "my-posts.gif",
            "notice-crud.gif",
            "post-crud.gif",
            "sign-up.gif",
            "upload-recognition.gif"
    );

    private static final long MAX_FILE_BYTES = 3L * 1024L * 1024L;
    private static final long MAX_TOTAL_BYTES = 15L * 1024L * 1024L;

    @Test
    public void readmeReferencesExactlyFourteenUniqueDemoGifs() throws IOException {
        String readme = read("README.md");
        Matcher matcher = DEMO_IMAGE.matcher(readme);
        List<String> references = new ArrayList<>();
        while (matcher.find()) {
            references.add(matcher.group(1));
        }

        assertEquals("README must reference exactly fourteen demo GIFs", 14, references.size());
        assertEquals("README demo references must be unique",
                references.size(), new LinkedHashSet<>(references).size());

        Set<String> referencedFiles = new TreeSet<>();
        for (String reference : references) {
            referencedFiles.add(Paths.get(reference).getFileName().toString());
        }
        assertEquals(EXPECTED_FILES, referencedFiles);
        assertEquals("docs/demo must not contain unreferenced GIFs",
                EXPECTED_FILES, actualDemoFiles());
    }

    @Test
    public void referencedDemoGifsAreValidAndStayWithinMediaBudget() throws IOException {
        Path demoDirectory = resolve("docs/demo").normalize();
        long totalBytes = 0L;

        for (String fileName : EXPECTED_FILES) {
            Path gif = demoDirectory.resolve(fileName).normalize();
            assertTrue(fileName + " must stay inside docs/demo", gif.startsWith(demoDirectory));
            assertTrue(fileName + " must exist", Files.isRegularFile(gif));

            long fileBytes = Files.size(gif);
            assertTrue(fileName + " must stay under 3 MiB", fileBytes <= MAX_FILE_BYTES);
            totalBytes += fileBytes;

            byte[] signature = new byte[6];
            try (InputStream input = Files.newInputStream(gif)) {
                int offset = 0;
                while (offset < signature.length) {
                    int read = input.read(signature, offset, signature.length - offset);
                    assertTrue(fileName + " has an incomplete GIF signature", read >= 0);
                    offset += read;
                }
            }
            String magic = new String(signature, StandardCharsets.US_ASCII);
            assertTrue(fileName + " must have a GIF signature",
                    "GIF87a".equals(magic) || "GIF89a".equals(magic));
        }

        assertTrue("all demo GIFs must stay under 15 MiB", totalBytes <= MAX_TOTAL_BYTES);
    }

    @Test
    public void readmeDropsLegacyDemoNamesAndKeepsPrivacyNotice() throws IOException {
        String readme = read("README.md");
        Path demoDirectory = resolve("docs/demo");

        assertTrue(readme.contains(
                "문서에 이름, 개인 계정, 외부 협업 문서와 개인 저장소 링크를 포함하지 않습니다."));
        for (String legacyFile : LEGACY_FILES) {
            assertFalse("README must not reference legacy demo " + legacyFile,
                    readme.contains("docs/demo/" + legacyFile));
            assertFalse("legacy demo must be removed: " + legacyFile,
                    Files.exists(demoDirectory.resolve(legacyFile)));
        }
    }

    private Set<String> actualDemoFiles() throws IOException {
        Set<String> files = new TreeSet<>();
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(resolve("docs/demo"), "*.gif")) {
            for (Path entry : entries) {
                files.add(entry.getFileName().toString());
            }
        }
        return files;
    }

    private String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(resolve(relativePath)), StandardCharsets.UTF_8);
    }

    private Path resolve(String relativePath) {
        return Paths.get(System.getProperty("user.dir")).resolve(relativePath);
    }
}
