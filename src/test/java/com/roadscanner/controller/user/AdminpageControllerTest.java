package com.roadscanner.controller.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ui.ExtendedModelMap;

import com.roadscanner.domain.user.MemberListPage;
import com.roadscanner.domain.user.MemberVO;
import com.roadscanner.service.user.AdminService;

public class AdminpageControllerTest {
    private AdminService adminService;
    private AdminpageController controller;

    @Before
    public void setUp() {
        adminService = mock(AdminService.class);
        controller = new AdminpageController(adminService);
    }

    @Test
    public void administratorListAlwaysExcludesAuthenticatedAdministrator() throws Exception {
        when(adminService.admin_searchCntBox("other", "current-admin")).thenReturn(1);
        when(adminService.admin(1, 5, "other", "current-admin"))
                .thenReturn(Collections.<MemberVO>emptyList());
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.listAdministrators(
                model, 1, "  other  ", member("current-admin", 2));

        assertThat(view).isEqualTo("login/list_admin");
        verify(adminService).admin_searchCntBox("other", "current-admin");
        verify(adminService).admin(1, 5, "other", "current-admin");
        assertThat(((MemberListPage) model.get("adminPage")).getKeyword()).isEqualTo("other");
    }

    @Test
    public void memberListUsesClampedPageOffset() throws Exception {
        when(adminService.member_searchCntBox("")).thenReturn(6);
        when(adminService.member(6, 5, "")).thenReturn(Collections.<MemberVO>emptyList());
        ExtendedModelMap model = new ExtendedModelMap();

        controller.listMembers(model, 999, "   ");

        verify(adminService).member(6, 5, "");
        assertThat(((MemberListPage) model.get("memberPage")).getPage()).isEqualTo(2);
    }

    @Test
    public void bannedListLimitsKeywordBeforeDatabaseLookup() throws Exception {
        String oversizedKeyword = repeat("가", 120);
        when(adminService.banned_searchCntBox(anyString())).thenReturn(0);
        when(adminService.banned(eq(1), eq(5), anyString()))
                .thenReturn(Collections.<MemberVO>emptyList());
        ExtendedModelMap model = new ExtendedModelMap();

        controller.listBannedMembers(model, 1, oversizedKeyword);

        MemberListPage page = (MemberListPage) model.get("bannedPage");
        assertThat(page.getKeyword()).hasSize(100);
        verify(adminService).banned_searchCntBox(page.getKeyword());
        verify(adminService).banned(1, 5, page.getKeyword());
    }

    private MemberVO member(String id, int grade) {
        MemberVO member = new MemberVO();
        member.setId(id);
        member.setGrade(grade);
        return member;
    }

    private String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder(value.length() * count);
        for (int index = 0; index < count; index++) {
            builder.append(value);
        }
        return builder.toString();
    }
}
