package com.roadscanner.view;

import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

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
    private static final int MIN_WIDTH = 1200;
    private static final int MIN_HEIGHT = 760;

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

            assertDemoQualityContract(gif, fileName);
        }

        assertTrue("all demo GIFs must stay under 15 MiB", totalBytes <= MAX_TOTAL_BYTES);
    }

    private void assertDemoQualityContract(Path gif, String fileName) throws IOException {
        try (ImageInputStream imageInput = ImageIO.createImageInputStream(gif.toFile())) {
            assertTrue(fileName + " must be readable by ImageIO", imageInput != null);
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            assertTrue(fileName + " must have a GIF reader", readers.hasNext());

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, false, false);
                assertTrue(fileName + " must be at least 1200px wide",
                        reader.getWidth(0) >= MIN_WIDTH);
                assertTrue(fileName + " must include the high-resolution caption area",
                        reader.getHeight(0) >= MIN_HEIGHT);
                int frameCount = reader.getNumImages(true);
                assertEquals(fileName + " must keep one frame per scene",
                        expectedSceneCount(fileName), frameCount);

                assertColorTableSize(
                        reader.getStreamMetadata(),
                        "javax_imageio_gif_stream_1.0",
                        "GlobalColorTable",
                        fileName + " global palette"
                );
                for (int frame = 0; frame < frameCount; frame++) {
                    IIOMetadata metadata = reader.getImageMetadata(frame);
                    Node control = metadataNode(
                            metadata,
                            "javax_imageio_gif_image_1.0",
                            "GraphicControlExtension"
                    );
                    assertTrue(fileName + " frame " + (frame + 1)
                            + " must have graphic control metadata", control != null);
                    assertEquals(fileName + " frame " + (frame + 1)
                                    + " must stay visible for 1500ms",
                            "150", nodeAttribute(control, "delayTime"));
                    assertEquals(fileName + " frame " + (frame + 1)
                                    + " must not use disposal transitions",
                            "doNotDispose", nodeAttribute(control, "disposalMethod"));

                    if (frame > 0) {
                        assertColorTableSize(
                                metadata,
                                "javax_imageio_gif_image_1.0",
                                "LocalColorTable",
                                fileName + " frame " + (frame + 1) + " local palette"
                        );
                    }
                }
            } finally {
                reader.dispose();
            }
        }
    }

    private void assertColorTableSize(
            IIOMetadata metadata,
            String format,
            String tableName,
            String description
    ) {
        Node table = metadataNode(metadata, format, tableName);
        assertTrue(description + " must exist", table != null);
        assertEquals(description + " must use 256 colors",
                "256", nodeAttribute(table, "sizeOfGlobalColorTable") != null
                        ? nodeAttribute(table, "sizeOfGlobalColorTable")
                        : nodeAttribute(table, "sizeOfLocalColorTable"));
    }

    private Node metadataNode(IIOMetadata metadata, String format, String nodeName) {
        assertTrue("GIF metadata must exist", metadata != null);
        return findNode(metadata.getAsTree(format), nodeName);
    }

    private Node findNode(Node current, String nodeName) {
        if (nodeName.equals(current.getNodeName())) {
            return current;
        }
        for (Node child = current.getFirstChild(); child != null; child = child.getNextSibling()) {
            Node match = findNode(child, nodeName);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private String nodeAttribute(Node node, String attributeName) {
        NamedNodeMap attributes = node.getAttributes();
        Node attribute = attributes == null ? null : attributes.getNamedItem(attributeName);
        return attribute == null ? null : attribute.getNodeValue();
    }

    private int expectedSceneCount(String fileName) {
        switch (fileName) {
            case "landing-overview.gif":
            case "admin-account-management.gif":
            case "image-analysis-flow.gif":
            case "qna-post-crud.gif":
                return 4;
            case "private-inquiry-list.gif":
                return 6;
            case "account-registration.gif":
            case "profile-management.gif":
                return 2;
            case "qna-notice-crud.gif":
                return 5;
            default:
                return 3;
        }
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
