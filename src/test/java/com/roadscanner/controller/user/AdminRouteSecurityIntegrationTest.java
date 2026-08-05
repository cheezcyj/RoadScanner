package com.roadscanner.controller.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.roadscanner.domain.user.MemberVO;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration("src/main/webapp")
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "ROADSCANNER_LOCAL_PASSWORD=LocalContext9!",
        "roadscanner.storage.delete-retry-delay-ms=3600000"
})
@ContextHierarchy({
        @ContextConfiguration(name = "root",
                locations = "file:src/main/webapp/WEB-INF/root-context.xml"),
        @ContextConfiguration(name = "servlet",
                locations = "file:src/main/webapp/WEB-INF/servlet-context.xml")
})
public class AdminRouteSecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    public void anonymousRequestsCannotReachConfiguredAdministratorRoutes() throws Exception {
        mockMvc.perform(get("/login/list_member"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/imgManagement"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/doDelete").param("idx", "1"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/local/mailbox"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/local/mailbox/clear"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/qna/inquiries"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void normalMemberCannotReachAdministratorRoutes() throws Exception {
        mockMvc.perform(get("/graph").session(sessionFor("localuser", 1)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/delete/").session(sessionFor("localuser", 1)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/local/mailbox").session(sessionFor("localuser", 1)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/local/mailbox/clear").session(sessionFor("localuser", 1)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/qna/inquiries").session(sessionFor("localuser", 1)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void trailingSlashVariantsRemainProtected() throws Exception {
        String[] getRoutes = {
                "/admin/", "/login/list_member/", "/login/list_admin/",
                "/login/list_banned/", "/graph/", "/monthlyFeedback/",
                "/totalFeedback/", "/doSelectOne/", "/imgManagement/",
                "/local/mailbox/", "/qna/inquiries/"
        };
        for (String route : getRoutes) {
            mockMvc.perform(get(route)).andExpect(status().isForbidden());
        }

        String[] postRoutes = {
                "/delete/", "/forbidden/", "/clear/", "/checkedUpdateMultiple/",
                "/doDeleteMultiple/", "/checkedUpdate/", "/doDelete/",
                "/local/mailbox/clear/"
        };
        for (String route : postRoutes) {
            mockMvc.perform(post(route)).andExpect(status().isForbidden());
        }
    }

    @Test
    public void administratorCanReachConfiguredAdministratorPage() throws Exception {
        mockMvc.perform(get("/graph").session(sessionFor("localadmin", 2)))
                .andExpect(status().isOk())
                .andExpect(view().name("graph"));

        mockMvc.perform(get("/qna/inquiries").session(sessionFor("localadmin", 2)))
                .andExpect(status().isOk())
                .andExpect(view().name("qna/index"));
    }

    @Test
    public void administratorCanReadAndClearLocalMailbox() throws Exception {
        MockHttpSession session = sessionFor("localadmin", 2);

        mockMvc.perform(get("/local/mailbox").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("local/mailbox"));
        mockMvc.perform(post("/local/mailbox/clear").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/local/mailbox"));
    }

    @Test
    public void administratorMemberListMappingsRenderTheirViews() throws Exception {
        MockHttpSession session = sessionFor("localadmin", 2);

        mockMvc.perform(get("/login/list_member").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("login/list_member"));
        mockMvc.perform(get("/login/list_admin").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("login/list_admin"));
        mockMvc.perform(get("/login/list_banned").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("login/list_banned"));
    }

    @Test
    public void questionWritePageRequiresAValidatedSession() throws Exception {
        mockMvc.perform(get("/qna/save"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/qna/save/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/qna/save").session(sessionFor("localuser", 1)))
                .andExpect(status().isOk())
                .andExpect(view().name("qna/question-save"));

        mockMvc.perform(get("/qna/inquiry/save"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/qna/inquiry/save").session(sessionFor("localuser", 1)))
                .andExpect(status().isOk())
                .andExpect(view().name("qna/question-save"));
    }

    @Test
    public void nestedQuestionApiIsCoveredBySessionRevalidation() throws Exception {
        mockMvc.perform(get("/api/qna/7/answer"))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpSession sessionFor(String id, int grade) {
        MockHttpSession session = new MockHttpSession();
        MemberVO member = new MemberVO();
        member.setId(id);
        member.setGrade(grade);
        session.setAttribute("user", member);
        return session;
    }
}
