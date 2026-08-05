package com.roadscanner.domain.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class MemberVOTest {

    @Test
    public void toStringNeverContainsPersonalInformation() {
        MemberVO member = new MemberVO("member", "top-secret-password", "member@example.com", 1);

        assertThat(member.toString())
                .doesNotContain("member")
                .doesNotContain("member@example.com")
                .doesNotContain("top-secret-password")
                .doesNotContain("password=");
    }
}
