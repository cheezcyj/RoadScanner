package com.roadscanner.dao.user;

import com.roadscanner.domain.user.MemberVO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/dao-test-context.xml")
@Transactional
public class AdminpageDaoImplTest {

    @Autowired
    private AdminpageDao adminpageDao;

    @Test
    public void memberListLoadsOnlyDisplayFields() throws Exception {
        List<MemberVO> members = adminpageDao.member(1, 5, "member");

        assertThat(members).hasSize(1);
        assertThat(members.get(0).getId()).isEqualTo("member01");
        assertThat(members.get(0).getEmail()).isEqualTo("member@example.test");
        assertThat(members.get(0).getPassword()).isNull();
    }

    @Test
    public void adminListUsesSingleValueExclusionAndDoesNotLoadPassword() throws Exception {
        List<MemberVO> admins = adminpageDao.admin(1, 5, "", "nobody");

        assertThat(admins).hasSize(1);
        assertThat(admins.get(0).getId()).isEqualTo("admin");
        assertThat(admins.get(0).getPassword()).isNull();
        assertThat(adminpageDao.admin(1, 5, "", "admin")).isEmpty();
    }

    @Test
    public void filteredListAndCountStayConsistentForEveryMemberType() throws Exception {
        List<MemberVO> members = adminpageDao.member(1, 5, "member");
        List<MemberVO> administrators = adminpageDao.admin(1, 5, "adm", "nobody");
        List<MemberVO> bannedMembers = adminpageDao.banned(1, 5, "banned");

        assertThat(adminpageDao.member_searchCntBox("member")).isEqualTo(members.size());
        assertThat(adminpageDao.admin_searchCntBox("adm", "nobody"))
                .isEqualTo(administrators.size());
        assertThat(adminpageDao.banned_searchCntBox("banned"))
                .isEqualTo(bannedMembers.size());
        assertThat(members).extracting(MemberVO::getId).containsExactly("member01");
        assertThat(administrators).extracting(MemberVO::getId).containsExactly("admin");
        assertThat(bannedMembers).extracting(MemberVO::getId).containsExactly("banned01");
        assertThat(bannedMembers.get(0).getPassword()).isNull();
    }
}
