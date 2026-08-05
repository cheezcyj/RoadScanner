package com.roadscanner.controller.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Validator;

import com.roadscanner.domain.user.MemberVO;
import com.roadscanner.service.user.EmailVerificationService;
import com.roadscanner.service.user.MailSendService;
import com.roadscanner.service.user.UserService;

public class EmailVerificationHttpTest {

    private UserService userService;
    private MailSendService mailSendService;
    private MockMvc mockMvc;

    @Before
    public void setUp() throws Exception {
        userService = mock(UserService.class);
        mailSendService = mock(MailSendService.class);
        EmailVerificationService verificationService = new EmailVerificationService();

        LoginController loginController = new LoginController();
        loginController.userService = userService;
        loginController.mailSend = mailSendService;
        loginController.emailVerificationService = verificationService;

        UserInfoController userInfoController = new UserInfoController();
        userInfoController.userService = userService;
        userInfoController.mailSend = mailSendService;
        userInfoController.emailVerificationService = verificationService;

        mockMvc = MockMvcBuilders
                .standaloneSetup(loginController, userInfoController)
                .setValidator(mock(Validator.class))
                .build();
    }

    @Test
    public void registrationRequiresPostCodeVerificationAndProofToken() throws Exception {
        MockHttpSession session = new MockHttpSession();
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);

        mockMvc.perform(get("/mailCheck").session(session).param("email", "member@example.com"))
                .andExpect(status().isMethodNotAllowed());

        MvcResult send = mockMvc.perform(post("/mailCheck")
                        .session(session)
                        .param("email", "member@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.msgId").value("10"))
                .andReturn();
        verify(mailSendService).sendRegistrationVerification(eq("member@example.com"), code.capture());
        org.junit.Assert.assertFalse(send.getResponse().getContentAsString().contains(code.getValue()));

        MvcResult verified = mockMvc.perform(post("/mailCheck/verify")
                        .session(session)
                        .param("email", "member@example.com")
                        .param("code", code.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msgId").value("10"))
				.andExpect(header().exists("X-Email-Verification-Token"))
				.andExpect(header().string("Cache-Control", "no-store"))
                .andReturn();
        String proofToken = proofToken(verified);
		org.junit.Assert.assertFalse(
				verified.getResponse().getContentAsString().contains(proofToken));

        mockMvc.perform(post("/register")
                        .session(session)
                        .param("id", "newmember1")
                        .param("password", "Password1!")
                        .param("email", "member@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msgId").value("20"));

        when(userService.register(any(MemberVO.class))).thenReturn(10);
        mockMvc.perform(post("/register")
                        .session(session)
                        .param("id", "newmember1")
                        .param("password", "Password1!")
                        .param("email", "member@example.com")
                        .param("verificationToken", proofToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msgId").value("10"));
    }

    @Test
    public void passwordResetRequiresVerifiedEmailProofToken() throws Exception {
        MockHttpSession session = new MockHttpSession();
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        when(userService.doEmailDuplCheck(any(MemberVO.class))).thenReturn(10);

        mockMvc.perform(post("/change_mailCheck")
                        .session(session)
                        .param("email", "Member@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msgId").value("10"));
        verify(mailSendService).sendPasswordResetVerification(eq("Member@example.com"), code.capture());

        MvcResult verified = mockMvc.perform(post("/change_mailCheck/verify")
                        .session(session)
                        .param("email", "Member@example.com")
                        .param("code", code.getValue()))
                .andExpect(status().isOk())
				.andExpect(header().exists("X-Email-Verification-Token"))
                .andReturn();

        mockMvc.perform(post("/changePassword")
                        .session(session)
                        .param("password", "Changed1!")
                        .param("email", "Member@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msgId").value("20"));

        when(userService.changePw(any(MemberVO.class))).thenReturn(1);
        mockMvc.perform(post("/changePassword")
                        .session(session)
                        .param("password", "Changed1!")
                        .param("email", "Member@example.com")
                        .param("verificationToken", proofToken(verified)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msgId").value("10"));
    }

    private String proofToken(MvcResult result) throws Exception {
        return result.getResponse().getHeader("X-Email-Verification-Token");
    }
}
