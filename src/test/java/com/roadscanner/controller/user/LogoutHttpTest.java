package com.roadscanner.controller.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class LogoutHttpTest {

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new LoginController()).build();
    }

    @Test
    public void postLogoutInvalidatesSessionAndRedirects() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", "authenticated-user");

        mockMvc.perform(post("/logout").session(session))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));

        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    public void getLogoutIsNotAllowed() throws Exception {
        mockMvc.perform(get("/logout"))
                .andExpect(status().isMethodNotAllowed());
    }
}
