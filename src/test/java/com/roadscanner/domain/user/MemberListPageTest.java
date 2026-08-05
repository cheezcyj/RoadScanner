package com.roadscanner.domain.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class MemberListPageTest {

    @Test
    public void emptyResultStillProducesStableFirstPage() {
        MemberListPage page = new MemberListPage(99, 0, null);

        assertThat(page.getPage()).isEqualTo(1);
        assertThat(page.getTotalPages()).isEqualTo(1);
        assertThat(page.getStartPage()).isEqualTo(1);
        assertThat(page.getEndPage()).isEqualTo(1);
        assertThat(page.getOffset()).isEqualTo(1);
        assertThat(page.isPrevious()).isFalse();
        assertThat(page.isNext()).isFalse();
        assertThat(page.getKeyword()).isEmpty();
    }

    @Test
    public void pageAndNavigationAreClampedToAvailableResults() {
        MemberListPage page = new MemberListPage(7, 63, "member");

        assertThat(page.getPage()).isEqualTo(7);
        assertThat(page.getTotalPages()).isEqualTo(13);
        assertThat(page.getStartPage()).isEqualTo(6);
        assertThat(page.getEndPage()).isEqualTo(10);
        assertThat(page.getPreviousPage()).isEqualTo(5);
        assertThat(page.getNextPage()).isEqualTo(11);
        assertThat(page.getOffset()).isEqualTo(31);
        assertThat(page.isPrevious()).isTrue();
        assertThat(page.isNext()).isTrue();
    }

    @Test
    public void oversizedAndNegativePageRequestsAreNormalized() {
        assertThat(new MemberListPage(-5, 8, "").getPage()).isEqualTo(1);
        assertThat(new MemberListPage(999, 8, "").getPage()).isEqualTo(2);
    }
}
