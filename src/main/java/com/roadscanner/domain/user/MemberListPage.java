package com.roadscanner.domain.user;

/**
 * Immutable paging state shared by the administrator member lists.
 */
public final class MemberListPage {
    private static final int DEFAULT_PAGE_SIZE = 5;
    private static final int DEFAULT_PAGE_LINK_SIZE = 5;

    private final int page;
    private final int count;
    private final int pageSize;
    private final int pageLinkSize;
    private final int totalPages;
    private final int startPage;
    private final int endPage;
    private final int offset;
    private final boolean previous;
    private final boolean next;
    private final String keyword;

    public MemberListPage(int requestedPage, int count, String keyword) {
        this.count = Math.max(0, count);
        this.pageSize = DEFAULT_PAGE_SIZE;
        this.pageLinkSize = DEFAULT_PAGE_LINK_SIZE;
        this.totalPages = Math.max(1, (this.count + pageSize - 1) / pageSize);
        this.page = Math.max(1, Math.min(requestedPage, totalPages));
        this.startPage = ((page - 1) / pageLinkSize) * pageLinkSize + 1;
        this.endPage = Math.min(startPage + pageLinkSize - 1, totalPages);
        this.offset = (page - 1) * pageSize + 1;
        this.previous = startPage > 1;
        this.next = endPage < totalPages;
        this.keyword = keyword == null ? "" : keyword;
    }

    public int getPage() {
        return page;
    }

    public int getCount() {
        return count;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getPageLinkSize() {
        return pageLinkSize;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getStartPage() {
        return startPage;
    }

    public int getEndPage() {
        return endPage;
    }

    public int getOffset() {
        return offset;
    }

    public boolean isPrevious() {
        return previous;
    }

    public boolean isNext() {
        return next;
    }

    public int getPreviousPage() {
        return Math.max(1, startPage - 1);
    }

    public int getNextPage() {
        return Math.min(totalPages, endPage + 1);
    }

    public String getKeyword() {
        return keyword;
    }
}
