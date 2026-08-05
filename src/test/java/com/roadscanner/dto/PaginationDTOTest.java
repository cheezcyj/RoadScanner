package com.roadscanner.dto;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PaginationDTOTest {

    @Test
    public void firstPageStartsAtZero() {
        PaginationDTO pagination = new PaginationDTO(1, 10);

        assertThat(pagination.getStart()).isZero();
    }

    @Test
    public void laterPageUsesPageSizeToCalculateOffset() {
        PaginationDTO pagination = new PaginationDTO(3, 20);

        assertThat(pagination.getStart()).isEqualTo(40);
    }

    @Test
    public void invalidValuesAreNormalized() {
        PaginationDTO pagination = new PaginationDTO(0, 0);

        assertThat(pagination.getPage()).isEqualTo(1);
        assertThat(pagination.getSize()).isEqualTo(10);
        assertThat(pagination.getStart()).isZero();
    }

    @Test
    public void pageSizeIsCapped() {
        PaginationDTO pagination = new PaginationDTO(2, 1_000);

        assertThat(pagination.getSize()).isEqualTo(100);
        assertThat(pagination.getStart()).isEqualTo(100);
    }
}
