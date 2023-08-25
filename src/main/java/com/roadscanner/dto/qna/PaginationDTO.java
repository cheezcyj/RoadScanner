package com.roadscanner.dto.qna;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter @Setter
public class PaginationDTO {
    private int page;
    private int size;

    public PaginationDTO(int page, int size) {
        this.page = page;
        this.size = size;
    }

    public int getStart() {
        return (page - 1) * size;
    }
}
