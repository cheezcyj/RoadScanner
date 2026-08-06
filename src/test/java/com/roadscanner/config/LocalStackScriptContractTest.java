package com.roadscanner.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

public class LocalStackScriptContractTest {

    @Test
    public void localStackScriptStartsAndStopsWebAndInferenceTogether() throws IOException {
        String script = read("scripts/start-local-stack.ps1");

        assertTrue(script.contains("'-m', 'ml_service.app'"));
        assertTrue(script.contains("-Droadscanner.local.spring-profiles=local,local-ml"));
        assertTrue(script.contains("-Droadscanner.local.public-base-url=http://127.0.0.1:$WebPort"));
        assertTrue(script.contains("ROADSCANNER_ML_PORT"));
        assertTrue(script.contains("ROADSCANNER_ALLOWED_IMAGE_PORTS"));
        assertTrue(script.contains("/health"));
        assertTrue(script.contains("/login"));
        assertTrue(script.contains("while (-not $webProcess.HasExited -and -not $mlProcess.HasExited)"));
        assertTrue(script.contains("Stop-ServiceProcess"));
        assertTrue(script.contains("-ExpectedProcessName 'java'"));
        assertTrue(script.contains("-ExpectedProcessName 'python'"));
        assertTrue(script.contains("if ($SmokeTest)"));
    }

    @Test
    public void localStackScriptDoesNotStoreCredentialsOrUserPaths() throws IOException {
        String script = read("scripts/start-local-stack.ps1");

        assertTrue(script.contains("Read-Host '로컬 테스트 비밀번호' -AsSecureString"));
        assertTrue(script.contains("$originalEnvironment['ROADSCANNER_LOCAL_PASSWORD']"));
        assertTrue(script.contains("Set-ProcessEnvironmentValue"));
        assertFalse(script.contains("ROADSCANNER_LOCAL_PASSWORD = '"));
        assertFalse(script.contains("-DROADSCANNER_LOCAL_PASSWORD="));
        assertFalse(script.matches("(?s).*[A-Za-z]:\\\\Users\\\\.*"));
    }

    @Test
    public void readmeRecommendsTheSingleStackCommand() throws IOException {
        String readme = read("README.md");
        String launcher = read("scripts/start-local-stack.cmd");

        assertTrue(readme.contains(".\\scripts\\start-local-stack.cmd"));
        assertTrue(readme.contains("어느 한쪽이 실패하거나 종료되면 나머지 프로세스도 함께 정리"));
        assertTrue(launcher.contains("-ExecutionPolicy Bypass"));
        assertTrue(launcher.contains("%~dp0start-local-stack.ps1"));
        assertFalse(launcher.matches("(?s).*[A-Za-z]:\\\\Users\\\\.*"));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
