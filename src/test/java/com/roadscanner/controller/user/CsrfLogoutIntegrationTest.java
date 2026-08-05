package com.roadscanner.controller.user;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.roadscanner.config.CsrfProtectionFilter;

public class CsrfLogoutIntegrationTest {

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new LoginController())
                .addFilters(new CsrfProtectionFilter())
                .build();
    }

    @Test
    public void logoutWithoutCsrfTokenIsRejectedAndSessionRemains() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", "authenticated");

        mockMvc.perform(post("/logout").session(session))
                .andExpect(status().isForbidden());

        assertFalse(session.isInvalid());
        assertNotNull(session.getAttribute("user"));
    }

    @Test
    public void logoutWithTokenInvalidatesSessionAndRedirects() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", "authenticated");
        MvcResult page = mockMvc.perform(get("/login").session(session))
                .andExpect(status().isOk())
                .andReturn();
        String token = (String) page.getRequest().getAttribute("csrfToken");
        assertNotNull(token);

        mockMvc.perform(post("/logout")
                        .session(session)
                        .header("X-CSRF-Token", token))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        assertTrue(session.isInvalid());
    }
}
