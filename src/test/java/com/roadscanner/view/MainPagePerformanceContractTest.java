package com.roadscanner.view;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MainPagePerformanceContractTest {

    @Test
    public void firstScreenUsesOptimizedAnimationWithGifFallback() throws IOException {
        String jsp = read("src/main/webapp/WEB-INF/views/login/main.jsp");
        Path originalGif = resolve("src/main/webapp/resources/video/driving.gif");
        Path optimizedAnimation = resolve("src/main/webapp/resources/video/driving-hero.webp");
        Path poster = resolve("src/main/webapp/resources/picture/driving-hero-poster.webp");

        assertTrue(jsp.contains("/resources/video/driving-hero.webp"));
        assertTrue(jsp.contains("/resources/video/driving.gif"));
        assertTrue(jsp.contains("/resources/picture/driving-hero-poster.webp"));
        assertTrue(Files.size(optimizedAnimation) * 8 < Files.size(originalGif));
        assertTrue(Files.size(poster) < 100 * 1024);
    }

    @Test
    public void belowFoldVideoLoadsOnlyNearItsSection() throws IOException {
        String jsp = read("src/main/webapp/WEB-INF/views/login/main.jsp");
        String javascript = read("src/main/webapp/resources/js/main.js");

        assertTrue(jsp.contains("class=\"background-video js-lazy-video\""));
        assertTrue(jsp.contains("preload=\"none\""));
        assertTrue(jsp.contains("data-src=\"${CP}/resources/video/driving.mp4\""));
        assertFalse(jsp.contains("class=\"background-video\" autoplay"));
        assertTrue(javascript.contains("IntersectionObserver"));
        assertTrue(javascript.contains("rootMargin: \"75% 0px\""));
    }

    @Test
    public void offscreenSectionsAndReducedMotionAvoidUnnecessaryRendering() throws IOException {
        String css = read("src/main/webapp/resources/css/main.css");

        assertTrue(css.contains("content-visibility: auto;"));
        assertTrue(css.contains("contain-intrinsic-size: auto 100svh;"));
        assertTrue(css.contains(".background-video {\n    display: none;"));
        assertFalse(css.contains(".background-video,\n  .background-gif"));
        assertFalse(blockStartingAt(css, ".site-header {").contains("backdrop-filter"));
    }

    private String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(resolve(relativePath)), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
    }

    private String blockStartingAt(String css, String selector) {
        int start = css.indexOf(selector);
        assertTrue("Expected selector was not found: " + selector, start >= 0);
        int end = css.indexOf('}', start);
        assertTrue("Expected block terminator was not found: " + selector, end > start);
        return css.substring(start, end + 1);
    }

    private Path resolve(String relativePath) {
        return Paths.get(System.getProperty("user.dir")).resolve(relativePath);
    }
}
