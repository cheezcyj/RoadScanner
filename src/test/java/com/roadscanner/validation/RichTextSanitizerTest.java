package com.roadscanner.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.roadscanner.cmn.validation.RichTextSanitizer;

public class RichTextSanitizerTest {

    @Test
    public void keepsSupportedFormatting() {
        String result = RichTextSanitizer.sanitize(
                "<h2>제목</h2><p><strong>중요</strong> 내용</p><ul><li>항목</li></ul>");

        assertThat(result).isEqualTo(
                "<h2>제목</h2><p><strong>중요</strong> 내용</p><ul><li>항목</li></ul>");
    }

    @Test
    public void removesExecutableMarkupAndAttributes() {
        String result = RichTextSanitizer.sanitize(
                "<p onclick=\"alert(1)\">안전<script>alert(2)</script>본문</p>"
                        + "<iframe src=\"https://example.invalid\"></iframe>"
                        + "<svg onload=\"alert(3)\"></svg>");

        assertThat(result).contains("안전").contains("본문");
        assertThat(result).doesNotContain("<script", "onclick", "<iframe", "<svg", "onload");
    }

    @Test
    public void preservesPlainTextAndLineBreaks() {
        assertThat(RichTextSanitizer.sanitize("첫 줄\n둘째 줄"))
                .isEqualTo("첫 줄\n둘째 줄");
    }

    @Test
    public void nullRemainsNull() {
        assertThat(RichTextSanitizer.sanitize(null)).isNull();
    }
}
