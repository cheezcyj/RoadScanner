<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@include file ="/WEB-INF/views/layout/header.jsp" %>
<body>
<%@include file ="/WEB-INF/views/layout/navbar.jsp" %>
    <div class="container mt-4">
            <c:choose>
                <c:when test="${user.grade == 2}">
                    <h1 class="mb-4"><a href="/qna" class="qna-title-link">Q&A 게시판(관리자)</a></h1>
                </c:when>
                <c:otherwise>
                     <h1 class="mb-4"><a href="/qna" class="qna-title-link">Q&A 게시판</a></h1>
                </c:otherwise>
            </c:choose>

        <!-- 필터링 조건이 존재하는지 확인하는 변수 -->
        <c:set var="isFiltered" value="${category != null or searchType != '' or keyword != ''}" />

        <form class="mb-3" name="searchFrm" action="/qna" method="get">
            <div class="row g-1 d-flex justify-content-between mt-3 mb-3">
                <!-- 공지 버튼 추가 -->
                <div class="col-auto d-flex align-items-start">
                    <c:if test="${!isFiltered and user.grade != 2}">
                        <button id="btn-toggle-notice" type="button" class="btn btn-outline-secondary">공지 보기</button>
                    </c:if>
                </div>
                <div class="col-auto flex-fill d-flex justify-content-end">
                    <!-- 분류 셀렉트 박스 -->
                    <div class="col-auto mx-1">
                        <select name="category" id="category" class="form-select">
                            <option value="">전체</option>
                            <option value="10" ${category == 10 ? 'selected' : ''}>공지</option>
                            <option value="20" ${category == 20 ? 'selected' : ''}>답변완료</option>
                            <option value="30" ${category == 30 ? 'selected' : ''}>답변대기</option>
                        </select>
                    </div>
                    <!-- 검색 셀렉트 박스 (오른쪽 정렬) -->
                    <div class="col-auto">
                        <select name="searchType" id="searchType" class="form-select">
                            <option value="title" ${searchType == 'title' ? 'selected' : ''}>제목</option>
                            <option value="content" ${searchType == 'content' ? 'selected' : ''}>내용</option>
                            <option value="both" ${searchType == 'both' ? 'selected' : ''}>제목+내용</option>
                        </select>
                    </div>
                    <!-- 검색 박스 (우측 마진 추가) -->
                    <div class="col-auto mx-1">
                        <div class="input-group">
                    <span class="input-group-text" id="basic-addon1">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-search" viewBox="0 0 16 16">
                            <path d="M11.742 10.344a6.5 6.5 0 1 0-1.397 1.398h-.001c.03.04.062.078.098.115l3.85 3.85a1 1 0 0 0 1.415-1.414l-3.85-3.85a1.007 1.007 0 0 0-.115-.1zM12 6.5a5.5 5.5 0 1 1-11 0 5.5 5.5 0 0 1 11 0z"></path>
                        </svg>
                    </span>
                            <input type="text" name="keyword" id="keyword" class="form-control" placeholder="검색어를 입력하세요">
                        </div>
                    </div>
                    <div>
                        <button type="submit" class="btn btn-secondary">검색</button>
                    </div>
                </div>
            </div>
        </form>

        <table class="table table-hover">
        <colgroup>
            <c:choose>
                <c:when test="${user.grade == 2}">
                    <col style="width: 5%;">
                    <col style="width: 5%;">
                    <col style="width: 15%;">
                    <col style="width: 35%;">
                    <col style="width: 15%;">
                    <col style="width: 15%;">
                    <col style="width: 15%;">
                </c:when>
                <c:otherwise>
                    <col style="width: 5%;">
                    <col style="width: 15%;">
                    <col style="width: 35%;">
                    <col style="width: 15%;">
                    <col style="width: 15%;">
                    <col style="width: 15%;">
                </c:otherwise>
            </c:choose>
        </colgroup>
           <thead class="table-group-divider">
               <tr>
                   <!-- 체크박스 컬럼 추가 (관리자만 보이게) -->
                   <c:if test="${user.grade == 2}">
                       <th class="text-center"><input type="checkbox" id="select-all"></th>
                   </c:if>
                   <th class="text-center">번호</th>
                   <th class="text-center">분류</th>
                   <th class="text-center">제목</th>
                   <th class="text-center">작성자</th>
                   <th class="text-center">작성일</th>
                   <th class="text-center">조회수</th>
               </tr>
            </thead>

            <tbody class="table-group-divider">
                <!-- 공지사항 출력 -->
                <c:if test="${!isFiltered and user.grade != 2}">
                    <c:forEach items="${notice}" var="notice">
                        <tr class="table notice-row" id="gongTable">
                            <td class="text-center">${notice.no}</td>
                            <td class="text-center"><span class="badge" id="gong">공지</span></td>
                            <td><a href="/qna/${notice.no}" class="text-dark qna-link notice-title">${notice.title}</a></td>
                            <td class="text-center">${notice.id}</td>
                            <td class="text-center">${notice.createDate}</td>
                            <td class="text-center">${notice.views}</td>
                        </tr>
                    </c:forEach>
                </c:if>

                <!-- 기존 질문 목록 출력 -->
                <c:forEach items="${questions}" var="question">
                    <c:choose>
                        <c:when test="${question.category == 10}">
                            <tr class="table" id="gongTable">
                                <c:if test="${user.grade == 2}">
                                   <td class="text-center">
                                       <input type="checkbox" class="delete-checkbox" value="${question.no}">
                                   </td>
                                </c:if>
                                <td class="text-center">${question.no}</td>
                                <td class="text-center">
                                <span class="badge" id="gong">공지</span></td>
                                <td><a href="/qna/${question.no}"
                                    class="text-dark qna-link notice-title">${question.title}</a></td>
                                <td class="text-center">${question.id}</td>
                                <td class="text-center">${question.createDate}</td>
                                <td class="text-center">${question.views}</td>
                            </tr>
                        </c:when>
                        <c:otherwise>
                            <tr>
                                <c:if test="${user.grade == 2}">
                                    <td class="text-center">
                                        <input type="checkbox" class="delete-checkbox" value="${question.no}">
                                    </td>
                                </c:if>
                                <td class="text-center">${question.no}</td>
                                <td class="text-center">
                                    <c:choose>
                                        <c:when test="${question.category == 20}">
                                            <span class="badge" id="wan">답변완료</span>
                                        </c:when>
                                        <c:when test="${question.category == 30}">
                                            <span class="badge" id="dae">답변대기</span>
                                        </c:when>
                                    </c:choose>
                                </td>
                                <td><a href="/qna/${question.no}" class="text-dark qna-link">${question.title}</a></td>
                                <td class="text-center">${question.id}</td>
                                <td class="text-center">${question.createDate}</td>
                                <td class="text-center">${question.views}</td>
                            </tr>
                        </c:otherwise>
                    </c:choose>
                </c:forEach>
            </tbody>
        </table>

        <!-- 글쓰기 버튼 -->
        <div class="d-flex justify-content-between align-items-center">
            <div class="mx-1">
                <!-- 삭제 버튼 추가 (관리자만 보이게) -->
                <c:if test="${user.grade == 2}">
                    <button type="button" class="btn btn-outline-secondary my-1" id="btn-delete-selected">선택삭제</button>
                </c:if>
            </div>
            <div class="ml-auto mx-1">
                <a href="/qna/save" class="btn btn-outline-secondary my-1" role="button">글쓰기</a>
            </div>
        </div>

        <!-- 페이징 시작 -->
        <nav aria-label="Page navigation">
            <ul class="pagination justify-content-center">
                <!-- 첫 페이지 및 이전 페이지 링크 -->
                <li class="page-item ${pagination.page <= 1 ? 'disabled' : ''}">
                    <a class="page-link" href="?page=1&size=${pagination.size}&searchType=${searchType}&keyword=${keyword}&category=${category}" aria-label="First">
                        &laquo;
                    </a>
                </li>
                <li class="page-item ${pagination.page <= 1 ? 'disabled' : ''}">
                    <a class="page-link" href="?page=${pagination.page - 1}&size=${pagination.size}&searchType=${searchType}&keyword=${keyword}&category=${category}" aria-label="Previous">
                        &lt;
                    </a>
                </li>
                <!-- 페이지 번호 링크 -->
                <c:forEach var="i" begin="${pagination.startPage}" end="${pagination.endPage}">
                    <li class="page-item ${i == pagination.page ? 'active' : ''}">
                        <a class="page-link" href="?page=${i}&size=${pagination.size}&searchType=${searchType}&keyword=${keyword}&category=${category}">${i}</a>
                    </li>
                </c:forEach>
                <!-- 다음 페이지 및 마지막 페이지 링크 -->
                <li class="page-item ${pagination.page >= pagination.totalPage ? 'disabled' : ''}">
                    <a class="page-link" href="?page=${pagination.page + 1}&size=${pagination.size}&searchType=${searchType}&keyword=${keyword}&category=${category}" aria-label="Next">
                        &gt;
                    </a>
                </li>
                <li class="page-item ${pagination.page >= pagination.totalPage ? 'disabled' : ''}">
                    <a class="page-link" href="?page=${pagination.totalPage}&size=${pagination.size}&searchType=${searchType}&keyword=${keyword}&category=${category}" aria-label="Last">
                        &raquo;
                    </a>
                </li>
            </ul>
        </nav>
        <!-- 페이징 끝 -->
    </div>
<%@include file ="/WEB-INF/views/layout/footer.jsp" %>
