package com.roadscanner.view;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MainPageServiceInfoContractTest {

    @Test
    public void serviceIntroductionMatchesTheSupportedClassifierScope() throws IOException {
        String jsp = read("src/main/webapp/WEB-INF/views/login/main.jsp");

        assertTrue(jsp.contains("독일 GTSRB 43종"));
        assertTrue(jsp.contains("지원 범위 밖인 이미지는 ‘인식 불가’"));
        assertTrue(jsp.contains("결과 확인·피드백"));
        assertTrue(jsp.contains("분류 결과 또는 인식 불가 안내"));

        assertFalse(jsp.contains("필요한 도로 정보를 찾습니다"));
        assertFalse(jsp.contains("결과 관리"));
    }

    @Test
    public void modelMetricsNameTheirDatasetAndEvaluationScope() throws IOException {
        String jsp = read("src/main/webapp/WEB-INF/views/login/main.jsp");

        assertTrue(jsp.contains("aria-label=\"GTSRB 분류 모델 검증 요약\""));
        assertTrue(jsp.contains("<strong>43종</strong>"));
        assertTrue(jsp.contains("학습 이미지 39,209장"));
        assertTrue(jsp.contains(">12,630</strong>"));
        assertTrue(jsp.contains("GTSRB 표지판 이미지"));
        assertTrue(jsp.contains(">99.01%</strong>"));
        assertTrue(jsp.contains("12,505 / 12,630 · 잘라낸 표지판 이미지 기준"));

        assertFalse(jsp.contains("<strong>40K</strong>"));
        assertFalse(jsp.contains("<strong>13K</strong>"));
        assertFalse(jsp.contains("<strong>98%</strong>"));
        assertFalse(jsp.contains("모델 정확도"));
        assertFalse(jsp.contains("aria-label=\"모델 데이터 요약\""));
    }

    @Test
    public void exactMetricValuesUseTheCompactResponsiveStyle() throws IOException {
        String jsp = read("src/main/webapp/WEB-INF/views/login/main.jsp");
        String css = read("src/main/webapp/resources/css/main.css");

        assertTrue(jsp.contains("/resources/css/main.css?v=14"));
        assertTrue(jsp.contains("class=\"metric-value-compact\">12,630</strong>"));
        assertTrue(jsp.contains("class=\"metric-value-compact\">99.01%</strong>"));
        assertTrue(css.contains(".metric-card .metric-value-compact {"));
        assertTrue(css.contains("font-variant-numeric: tabular-nums;"));
        assertTrue(css.contains("white-space: nowrap;"));
    }

    @Test
    public void serviceIntroductionUsesACompactSpacingRhythm() throws IOException {
        String css = read("src/main/webapp/resources/css/main.css");

        assertTrue(css.contains(".hero-content > .eyebrow {\n  margin-bottom: 16px;\n}"));
        assertTrue(css.contains(".hero-content h2 span {\n  display: inline-block;\n  margin-top: 6px;"));
        assertTrue(css.contains(".hero-description {\n  max-width: 660px;\n  margin: 22px 0 0;"));
        assertTrue(css.contains(".hero-actions {\n  margin-top: 26px;\n}"));
        assertTrue(css.contains("gap: 12px 26px;\n  margin: 30px 0 0;"));
        assertTrue(css.contains(".hero-content > .eyebrow {\n    margin-bottom: 14px;\n  }"));
        assertTrue(css.contains(".hero-content h2 span {\n    margin-top: 4px;\n  }"));
        assertTrue(css.contains(".hero-description {\n    margin-top: 18px;\n    font-size: 15px;"));
        assertTrue(css.contains(".hero-actions {\n    margin-top: 22px;\n  }"));
        assertTrue(css.contains("gap: 9px 18px;\n    margin-top: 24px;"));
    }

    @Test
    public void howItWorksUsesACompactSpacingRhythm() throws IOException {
        String css = read("src/main/webapp/resources/css/main.css");

        assertTrue(css.contains(".about-copy > .eyebrow {\n  margin-bottom: 16px;\n}"));
        assertTrue(css.contains(".about-copy > p:not(.eyebrow) {\n  max-width: 650px;\n  margin: 20px 0 0;"));
        assertTrue(css.contains("grid-template-columns: repeat(3, minmax(0, 1fr));\n  gap: 12px;\n  margin: 26px 0 0;"));
        assertTrue(css.contains(".about-copy > .eyebrow {\n    margin-bottom: 14px;\n  }"));
        assertTrue(css.contains(".about-copy > p:not(.eyebrow) {\n    margin-top: 18px;\n  }"));
        assertTrue(css.contains("grid-template-columns: 1fr;\n    margin-top: 24px;"));
        assertTrue(css.contains(".about-layout {\n    gap: 30px;\n  }"));
        assertTrue(css.contains("display: grid;\n  align-content: start;\n  row-gap: 8px;"));
        assertTrue(css.contains("content: \"0\" counter(process);\n  display: block;\n  margin-bottom: 0;"));
        assertTrue(css.contains(".process-list strong {\n  margin-bottom: 0;"));
        assertTrue(css.contains("flex-direction: column;\n  gap: 8px;\n  margin: 18px 0 0;"));
        assertTrue(css.contains(".process-list li {\n    row-gap: 7px;\n    padding: 16px;"));
    }

    private String read(String relativePath) throws IOException {
        Path path = Paths.get(System.getProperty("user.dir")).resolve(relativePath);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
    }
}
