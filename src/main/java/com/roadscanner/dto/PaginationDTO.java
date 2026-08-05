package com.roadscanner.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter @Setter
public class PaginationDTO {
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private int page;
    private int size;

    public PaginationDTO(int page, int size) {
        this.page = Math.max(page, 1);
        this.size = normalizeSize(size);
    }

    public int getStart() {
        return (page - 1) * size;
    }

    private int normalizeSize(int requestedSize) {
        if (requestedSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(requestedSize, MAX_PAGE_SIZE);
    }
}
