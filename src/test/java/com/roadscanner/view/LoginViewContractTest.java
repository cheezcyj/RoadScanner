package com.roadscanner.view;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class LoginViewContractTest {

    @Test
    public void loginRequestAlwaysSendsThePageCsrfToken() throws IOException {
        String header = read("src/main/webapp/WEB-INF/views/layout/header.jsp");
        String login = read("src/main/webapp/WEB-INF/views/login/login.jsp");
        String script = read("src/main/webapp/resources/js/login/login.js");

        assertTrue(header.contains("/resources/js/csrf.js?v=2"));
        assertTrue(login.contains("/resources/js/login/login.js?v=4"));
        assertTrue(login.contains("action=\"${CP}/login\" method=\"post\""));
        assertTrue(login.contains("name=\"${csrfParameterName}\" value=\"${csrfToken}\""));
        assertTrue(script.contains("meta[name=\"csrf-token\"]"));
        assertTrue(script.contains("meta[name=\"csrf-parameter\"]"));
        assertTrue(script.contains("loginData[csrfParameter] = csrfToken;"));
        assertTrue(script.contains("data: loginData"));
    }

    @Test
    public void everyPasswordFormUsesTheSharedCredentialPolicy() throws IOException {
        String registration = read("src/main/webapp/WEB-INF/views/login/registerpage.jsp");
        String reset = read("src/main/webapp/WEB-INF/views/login/changePw.jsp");
        String mypage = read("src/main/webapp/WEB-INF/views/login/mypage.jsp");
        String policy = read("src/main/webapp/resources/js/login/password-policy.js");

        assertTrue(registration.contains("/resources/js/login/password-policy.js?v=1"));
        assertTrue(reset.contains("/resources/js/login/password-policy.js?v=1"));
        assertTrue(mypage.contains("/resources/js/login/password-policy.js?v=1"));
        assertTrue(policy.contains("hasLetter && hasDigit && hasSpecial"));
        assertTrue(policy.contains("characters.length < 8 || characters.length > 20"));
    }

    @Test
    public void mypageRequiresAndSendsTheCurrentPassword() throws IOException {
        String mypage = read("src/main/webapp/WEB-INF/views/login/mypage.jsp");
        String script = read("src/main/webapp/resources/js/login/mypage.js");

        assertTrue(mypage.contains("id=\"currentPassword\""));
        assertTrue(mypage.contains("autocomplete=\"current-password\""));
        assertTrue(script.contains("currentPassword: $(\"#currentPassword\").val()"));
        assertTrue(script.contains("window.location.href = myPageUrl(\"/login\")"));
    }

    private String read(String relativePath) throws IOException {
        Path path = Paths.get(System.getProperty("user.dir")).resolve(relativePath);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
