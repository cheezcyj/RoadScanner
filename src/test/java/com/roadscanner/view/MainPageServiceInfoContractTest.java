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

        assertTrue(jsp.contains("/resources/css/main.css?v=12"));
        assertTrue(jsp.contains("class=\"metric-value-compact\">12,630</strong>"));
        assertTrue(jsp.contains("class=\"metric-value-compact\">99.01%</strong>"));
        assertTrue(css.contains(".metric-card .metric-value-compact {"));
        assertTrue(css.contains("font-variant-numeric: tabular-nums;"));
        assertTrue(css.contains("white-space: nowrap;"));
    }

    private String read(String relativePath) throws IOException {
        Path path = Paths.get(System.getProperty("user.dir")).resolve(relativePath);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
    }
}
