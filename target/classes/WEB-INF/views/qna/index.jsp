<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>로드스캐너</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="/resources/css/qna.css" rel="stylesheet">
</head>
<body>
<div class="container mt-4">
    <h1>Q&A 게시판</h1>
    <div class="mb-3">
        <a href="/qna/save" class="btn btn-primary" role="button">글쓰기</a>
    </div>
    <table class="table table-striped">
        <thead>
        <tr>
            <th>번호</th>
            <th>분류</th>
            <th>제목</th>
            <th>작성자</th>
            <th>작성일</th>
            <th>조회수</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${questions}" var="question">
            <c:choose>
                <c:when test="${question.category == 10}">
                    <tr class="table-info">
                        <td>${question.no}</td>
                        <td><span class="badge bg-primary">공지</span></td>
                        <td><a href="/qna/${question.no}" class="text-dark qna-link notice-title">${question.title}</a></td>
                        <td>${question.id}</td>
                        <td>${question.createDate}</td>
                        <td>${question.views}</td>
                    </tr>
                </c:when>
                <c:otherwise>
                    <tr>
                        <td>${question.no}</td>
                        <td>
                            <c:choose>
                                <c:when test="${question.category == 20}">
                                    <span class="badge bg-success">답변완료</span>
                                </c:when>
                                <c:when test="${question.category == 30}">
                                    <span class="badge bg-light text-dark">답변대기</span>
                                </c:when>
                            </c:choose>
                        </td>
                        <td><a href="/qna/${question.no}" class="text-dark qna-link">${question.title}</a></td>
                        <td>${question.id}</td>
                        <td>${question.createDate}</td>
                        <td>${question.views}</td>
                    </tr>
                </c:otherwise>
            </c:choose>
        </c:forEach>
        </tbody>
    </table><!-- ... -->
    <!-- 생략 -->
    <nav aria-label="Page navigation">
        <ul class="pagination justify-content-center">
            <!-- 이전 페이지 버튼 -->
            <li class="page-item ${page <= 1 ? 'disabled' : ''}">
                <a class="page-link" href="${page > 1 ? '/qna?page='.concat(page - 1).concat('&size=10') : '#'}" aria-label="Previous">
                    <span aria-hidden="true">&laquo;</span>
                </a>
            </li>
            <!-- 페이지 번호 -->
            <c:forEach begin="1" end="${totalPages}" var="pageNum">
                <li class="page-item"><a class="page-link" href="/qna?page=${pageNum}&size=10">${pageNum}</a></li>
            </c:forEach>
            <!-- 다음 페이지 버튼 -->
            <li class="page-item ${page >= totalPages ? 'disabled' : ''}">
                <a class="page-link" href="${page < totalPages ? '/qna?page='.concat(page + 1).concat('&size=10') : '#'}" aria-label="Next">
                    <span aria-hidden="true">&raquo;</span>
                </a>
            </li>
        </ul>
        <!-- 마지막 페이지 안내 메시지 -->
        <c:if test="${page == totalPages}">
            <p class="text-center">마지막 페이지입니다.</p>
        </c:if>
    </nav>
    <!-- 생략 -->
</div>
</body>
</html>