package com.roadscanner.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
    public void preservesControlCharactersInParsedTagNames() {
        Document document = Jsoup.parse("<template\u001E><select><input>");

        assertThat(document.body().child(0).normalName()).isEqualTo("template\u001E");
    }

    @Test
    public void rejectsControlTerminatedRawTextMarkup() {
        String sanitized = RichTextSanitizer.sanitize(
                "<p>before</p><style\u001E><img src=x onerror=alert(1)>"
                        + "</style\u001E><p>after</p>");
        Document reparsed = Jsoup.parseBodyFragment(sanitized);

        assertThat(sanitized).isEqualTo("<p>before</p><p>after</p>");
        assertThat(reparsed.select(
                "style, script, img, iframe, svg, [onerror], [onload]")).isEmpty();
        assertThat(sanitized).doesNotContain("\u001E", "onerror", "<img");
        assertThat(RichTextSanitizer.sanitize(sanitized)).isEqualTo(sanitized);
    }

    @Test
    public void nullRemainsNull() {
        assertThat(RichTextSanitizer.sanitize(null)).isNull();
    }
}
