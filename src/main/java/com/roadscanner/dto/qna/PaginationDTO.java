package com.roadscanner.dto.qna;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter @Setter
public class PaginationDTO {
    private int page; // 현재 페이지
    private int size; // 페이지당 보여줄 게시글 수
    private int totalPage; // 총 페이지 수
    private int startPage; // 시작 페이지
    private int endPage; // 끝 페이지
    private boolean hasPrev; // 이전 페이지 존재 여부
    private boolean hasNext; // 다음 페이지 존재 여부

    private static final int PAGE_BLOCK = 5; // 한 번에 표시할 페이지 수

    public PaginationDTO(int page, int size, int countQuestions) {
        this.page = page;
        this.size = size;
        this.totalPage = (int) Math.ceil((double) countQuestions / size);

        // 시작 페이지와 끝 페이지 계산 로직
        if (totalPage <= PAGE_BLOCK) {
            this.startPage = 1;
            this.endPage = totalPage;
        } else {
            this.startPage = ((page - 1) / PAGE_BLOCK) * PAGE_BLOCK + 1;
            this.endPage = Math.min(this.startPage + PAGE_BLOCK - 1, totalPage);
        }

        // 이전/다음 페이지 존재 여부 설정
        this.hasPrev = startPage > 1;
        this.hasNext = endPage < totalPage;
    }

    public int getStart() {
        return (page - 1) * size;
    }

}
