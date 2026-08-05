<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<c:set var="pageStylesheet" value="/resources/css/qna.css?v=11" />
<%@include file="/WEB-INF/views/layout/header.jsp" %>
<c:set var="resolvedViewMode"
       value="${not empty viewMode ? viewMode : (mineOnly ? 'myInquiry' : (adminInquiryMode ? 'adminInquiry' : 'board'))}" />
<c:set var="inquiryList" value="${resolvedViewMode eq 'myInquiry' or resolvedViewMode eq 'adminInquiry'}" />
<c:set var="resolvedListPath"
       value="${not empty listPath ? listPath : (resolvedViewMode eq 'myInquiry' ? '/qna/my' : (resolvedViewMode eq 'adminInquiry' ? '/qna/inquiries' : '/qna'))}" />
<body class="rs-internal-page d-flex flex-column min-vh-100">
<%@include file="/WEB-INF/views/layout/navbar.jsp" %>

<main class="rs-page-shell">
    <header class="rs-page-heading">
        <c:choose>
            <c:when test="${resolvedViewMode eq 'myInquiry'}">
                <p class="rs-page-eyebrow">MY INQUIRIES</p>
                <h1 class="rs-page-title"><a href="${CP}${resolvedListPath}" class="qna-title-link">내 문의글 보기</a></h1>
                <p class="rs-page-description">내가 작성한 문의와 관리자 답변 상태를 확인할 수 있습니다.</p>
            </c:when>
            <c:when test="${resolvedViewMode eq 'adminInquiry'}">
                <p class="rs-page-eyebrow">INQUIRY MANAGEMENT</p>
                <h1 class="rs-page-title"><a href="${CP}${resolvedListPath}" class="qna-title-link">문의 관리</a></h1>
                <p class="rs-page-description">사용자 문의를 확인하고 답변 상태를 관리합니다.</p>
            </c:when>
            <c:otherwise>
                <p class="rs-page-eyebrow">ROADSCANNER Q&amp;A</p>
                <h1 class="rs-page-title"><a href="${CP}${resolvedListPath}" class="qna-title-link">Q&amp;A 게시판</a></h1>
                <p class="rs-page-description">공지와 일반 게시글을 확인하고 필요한 정보를 나눌 수 있습니다.</p>
            </c:otherwise>
        </c:choose>
    </header>

    <form class="qna-search-form mb-4" name="searchFrm" action="${CP}${resolvedListPath}" method="get">
        <input type="hidden" name="size" value="${size}">
        <div class="qna-search-row row g-2 justify-content-end">
            <div class="col-auto">
                <label class="visually-hidden" for="category">분류</label>
                <select class="form-select" name="category" id="category">
                    <c:choose>
                        <c:when test="${inquiryList}">
                            <option value="">전체 상태</option>
                            <option value="answered" ${category eq 'answered' ? 'selected' : ''}>답변 완료</option>
                            <option value="waiting" ${category eq 'waiting' ? 'selected' : ''}>답변 대기</option>
                        </c:when>
                        <c:otherwise>
                            <option value="">전체 분류</option>
                            <option value="notice" ${category eq 'notice' ? 'selected' : ''}>공지</option>
                            <option value="general" ${category eq 'general' ? 'selected' : ''}>일반</option>
                        </c:otherwise>
                    </c:choose>
                </select>
            </div>
            <div class="col-auto">
                <label class="visually-hidden" for="searchType">검색 대상</label>
                <select class="form-select" name="searchType" id="searchType">
                    <option value="both" ${searchType eq 'both' ? 'selected' : ''}>제목+내용</option>
                    <option value="title" ${searchType eq 'title' ? 'selected' : ''}>제목</option>
                    <option value="content" ${searchType eq 'content' ? 'selected' : ''}>내용</option>
                </select>
            </div>
            <div class="col-auto">
                <label class="visually-hidden" for="keyword">검색어</label>
                <div class="input-group">
                    <span class="input-group-text" aria-hidden="true">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"
                             fill="currentColor" viewBox="0 0 16 16">
                            <path d="M11.742 10.344a6.5 6.5 0 1 0-1.397 1.398h-.001c.03.04.062.078.098.115l3.85 3.85a1 1 0 0 0 1.415-1.414l-3.85-3.85a1.007 1.007 0 0 0-.115-.1zM12 6.5a5.5 5.5 0 1 1-11 0 5.5 5.5 0 0 1 11 0z"/>
                        </svg>
                    </span>
                    <input type="search" name="keyword" class="form-control" id="keyword"
                           maxlength="100" value="<c:out value="${keyword}"/>"
                           placeholder="검색어를 입력하세요">
                </div>
            </div>
            <div class="col-auto">
                <button type="submit" class="btn btn-primary">검색</button>
            </div>
        </div>
    </form>

    <div class="table-responsive">
        <table class="table table-hover">
            <thead>
            <tr>
                <th class="text-center">번호</th>
                <th class="text-center">분류</th>
                <th class="text-center">제목</th>
                <th class="text-center">작성자</th>
                <th class="text-center">작성일</th>
                <th class="text-center">조회수</th>
            </tr>
            </thead>
            <tbody class="table-group-divider">
            <c:forEach items="${questions}" var="question">
                <tr class="${question.category == 10 ? 'table notice-row' : ''}">
                    <td class="text-center"><c:out value="${question.no}"/></td>
                    <td class="text-center">
                        <c:choose>
                            <c:when test="${question.category == 10}">
                                <span class="badge" style="background-color:#F87217;color:white;">공지</span>
                            </c:when>
                            <c:when test="${question.category == 20}">
                                <span class="badge" style="background-color:#024089;color:white;">답변 완료</span>
                            </c:when>
                            <c:when test="${question.category == 30}">
                                <span class="badge" style="background-color:#E0EAF5;color:black;">답변 대기</span>
                            </c:when>
                            <c:when test="${question.category == 40}">
                                <span class="badge bg-secondary">일반</span>
                            </c:when>
                            <c:otherwise>
                                <span class="badge bg-secondary">기타</span>
                            </c:otherwise>
                        </c:choose>
                    </td>
                    <td>
                        <a href="${CP}/qna/${question.no}"
                           class="text-dark qna-link ${question.category == 10 ? 'notice-title' : ''}">
                            <c:out value="${question.title}"/>
                        </a>
                    </td>
                    <td class="text-center"><c:out value="${question.id}"/></td>
                    <td class="text-center"><c:out value="${question.createDate}"/></td>
                    <td class="text-center"><c:out value="${question.views}"/></td>
                </tr>
            </c:forEach>
            <c:if test="${empty questions}">
                <tr>
                    <td colspan="6" class="text-center py-4">
                        <c:choose>
                            <c:when test="${resolvedViewMode eq 'myInquiry'}">작성한 문의가 없습니다.</c:when>
                            <c:when test="${resolvedViewMode eq 'adminInquiry'}">조건에 맞는 문의가 없습니다.</c:when>
                            <c:otherwise>조건에 맞는 게시글이 없습니다.</c:otherwise>
                        </c:choose>
                    </td>
                </tr>
            </c:if>
            </tbody>
        </table>
    </div>

    <c:choose>
        <c:when test="${resolvedViewMode eq 'myInquiry'}">
            <div class="d-flex justify-content-end qna-list-actions">
                <a href="${CP}/qna/inquiry/save" class="btn btn-outline-primary qna-write-button" role="button">문의글쓰기</a>
            </div>
        </c:when>
        <c:when test="${resolvedViewMode eq 'board'}">
            <div class="d-flex justify-content-end qna-list-actions">
                <a href="${CP}/qna/save" class="btn btn-outline-primary qna-write-button" role="button">Q&amp;A 글쓰기</a>
            </div>
        </c:when>
    </c:choose>

    <c:set var="previousPage" value="${page > 1 ? page - 1 : 1}"/>
    <c:set var="nextPage" value="${page < totalPages ? page + 1 : totalPages}"/>
    <c:url var="firstPageUrl" value="${resolvedListPath}">
        <c:param name="page" value="1"/>
        <c:param name="size" value="${size}"/>
        <c:param name="category" value="${category}"/>
        <c:param name="searchType" value="${searchType}"/>
        <c:param name="keyword" value="${keyword}"/>
    </c:url>
    <c:url var="previousPageUrl" value="${resolvedListPath}">
        <c:param name="page" value="${previousPage}"/>
        <c:param name="size" value="${size}"/>
        <c:param name="category" value="${category}"/>
        <c:param name="searchType" value="${searchType}"/>
        <c:param name="keyword" value="${keyword}"/>
    </c:url>
    <c:url var="nextPageUrl" value="${resolvedListPath}">
        <c:param name="page" value="${nextPage}"/>
        <c:param name="size" value="${size}"/>
        <c:param name="category" value="${category}"/>
        <c:param name="searchType" value="${searchType}"/>
        <c:param name="keyword" value="${keyword}"/>
    </c:url>
    <c:url var="lastPageUrl" value="${resolvedListPath}">
        <c:param name="page" value="${totalPages}"/>
        <c:param name="size" value="${size}"/>
        <c:param name="category" value="${category}"/>
        <c:param name="searchType" value="${searchType}"/>
        <c:param name="keyword" value="${keyword}"/>
    </c:url>

    <nav class="qna-pagination" aria-label="페이지 이동">
        <ul class="pagination justify-content-center" id="pagination">
            <li class="page-item ${page <= 1 ? 'disabled' : ''}">
                <a class="page-link" href="${firstPageUrl}" aria-label="첫 페이지">&laquo;&laquo;</a>
            </li>
            <li class="page-item ${page <= 1 ? 'disabled' : ''}">
                <a class="page-link" href="${previousPageUrl}" aria-label="이전 페이지">&laquo;</a>
            </li>
            <c:forEach begin="1" end="${totalPages}" var="pageNum">
                <c:url var="pageUrl" value="${resolvedListPath}">
                    <c:param name="page" value="${pageNum}"/>
                    <c:param name="size" value="${size}"/>
                    <c:param name="category" value="${category}"/>
                    <c:param name="searchType" value="${searchType}"/>
                    <c:param name="keyword" value="${keyword}"/>
                </c:url>
                <li class="page-item ${page == pageNum ? 'active' : ''}">
                    <a class="page-link" href="${pageUrl}"><c:out value="${pageNum}"/></a>
                </li>
            </c:forEach>
            <li class="page-item ${page >= totalPages ? 'disabled' : ''}">
                <a class="page-link" href="${nextPageUrl}" aria-label="다음 페이지">&raquo;</a>
            </li>
            <li class="page-item ${page >= totalPages ? 'disabled' : ''}">
                <a class="page-link" href="${lastPageUrl}" aria-label="마지막 페이지">&raquo;&raquo;</a>
            </li>
        </ul>
    </nav>
</main>

<%@include file="/WEB-INF/views/layout/footer.jsp" %>
