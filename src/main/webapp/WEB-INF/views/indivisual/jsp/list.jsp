<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<html>
<head>
<title>로드스캐너 - 게시판</title>
<meta charset="UTF-8">
<meta name="viewport"
    content="width=device-width, initial-scale=1, shrink-to-fit=no">
<!-- 부트스트랩 CSS 추가 -->
<link
    href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css"
    rel="stylesheet">
<link href="/resources/css/qna.css" rel="stylesheet">
<link  href="${c}/resources/css/membership-style.css" rel="stylesheet">
<link  href="${c}/resources/css/bootstrap/bootstrap.min.css" rel="stylesheet"  crossorigin="anonymous">
<script src="${c}/resources/js/bootstrap/bootstrap.bundle.min.js"  crossorigin="anonymous"></script>
</head>
<nav class="navbar navbar-expand-md mb-4" style="background-color: white;">
  <div class="container-fluid">
    <a class="navbar-brand" href="#">RoadScanner</a>
    <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarCollapse" aria-controls="navbarCollapse" aria-expanded="false" aria-label="Toggle navigation">
      <span class="navbar-toggler-icon"></span>
    </button>
    <div class="collapse navbar-collapse" id="navbarCollapse">
      <ul class="navbar-nav me-auto mb-2 mb-md-0">
        <c:if test="${user ne null}">
          <li class="nav-item">
            <a class="nav-link" href="${c}/main/preUpload">사진 업로드</a>
          </li>
          <li class="nav-item">
            <a class="nav-link" href="${c}/qna">게시판</a>
          </li>
        </c:if>
        <c:if test="${user.grade == 2}">
        <li class="nav-item dropdown">
          <input type="hidden" id="nekeyword" name="nekeyword" value ="${user.id}">
          <a class="nav-link dropdown-toggle" href="#" data-bs-toggle="dropdown" aria-expanded="false">관리자 기능</a>
          <ul class="dropdown-menu">
            <li><a class="dropdown-item" href="${c}/admin">List</a></li>
            <li><a class="dropdown-item" href="#">Upload</a></li>
            <li><a class="dropdown-item" href="#">None</a></li>
          </ul>
        </li>
        </c:if>
      </ul>
      <form class="d-flex" role="search">
        <!-- 로그인 세션 X -->
        <c:if test="${user eq null}">
          <button type="button" id="login" onclick="location.href='${c}/login'" class="btn btn-outline-primary me-2">Login</button>
          <button type="button" onclick="location.href='${c}/registerpage'" class="btn btn-outline-primary" style="margin-right: 50px;">Sign-up</button>
        </c:if>
        <!-- 로그인 세션 O -->
        <c:if test="${user ne null}">
          <button type="button" class="btn btn-outline-primary me-2" onclick="location.href='${c}/mypage'">MyPage</button>
          <button type="button" class="btn btn-outline-primary" onclick="location.href='${c}/logout'" style="margin-right: 50px;">LogOut</button>
        </c:if>
      </form>
    </div>
  </div>
</nav>
<body>
    <div class="container mt-4">
        <h1 class="mb-4">Q&A 게시판</h1>
        <form name="searchFrm">
            <div class="row g-1 d-flex justify-content-between mt-3 mb-3">
                <div class="col-auto flex-fill justify-content-end d-flex">
                    <!-- 분류 셀렉트 박스 -->
                    <div class="col-auto mx-1">
                        <select class="form-select" name="category" id="category">
                            <option value="">분류</option>
                            <option value="공지">공지</option>
                            <option value="답변대기">답변대기</option>
                            <option value="답변완료">답변완료</option>
                        </select>
                    </div>
                    <!-- 검색 박스와 글쓰기 버튼을 포함한 레이아웃 -->
                    <!-- 검색 셀렉트 박스 (오른쪽 정렬) -->
                    <div class="col-auto">
                        <select class="form-select" name="searchType" id="searchType">
                            <option value="both">제목+내용</option>
                            <option value="title">제목</option>
                            <option value="content">내용</option>
                        </select>
                    </div>
                    <!-- 검색 박스 (우측 마진 추가) -->
                    <div class="col-auto mx-1">
                        <div class="input-group">
                            <span class="input-group-text" id="basic-addon1"> 
                            <svg
                                    xmlns="http://www.w3.org/2000/svg" width="16" height="16"
                                    fill="currentColor" class="bi bi-search" viewBox="0 0 16 16">
                              <path d="M11.742 10.344a6.5 6.5 0 1 0-1.397 1.398h-.001c.03.04.062.078.098.115l3.85 3.85a1 1 0 0 0 1.415-1.414l-3.85-3.85a1.007 1.007 0 0 0-.115-.1zM12 6.5a5.5 5.5 0 1 1-11 0 5.5 5.5 0 0 1 11 0z"></path>
                            </svg>
                            </span> <input type="text" name="keyword" class="form-control" id="keyword"
                                placeholder="검색어를 입력하세요.">
                        </div>
                    </div>
                    <div>
                        <button type="submit" class="btn btn-primary">검색</button>
                    </div>
                </div>
            </div>
        </form>
        <table class="table table-hover">
            <thead class="table-group-divider" style="border-color: #DCDCDC;">
                <tr>
                    <th class="text-center">번호</th>
                    <th class="text-center">분류</th>
                    <th class="text-center">제목</th>
                    <th class="text-center">작성자</th>
                    <th class="text-center">작성일</th>
                    <th class="text-center">조회수</th>
                </tr>
            </thead>
            <tbody class="table-group-divider" style="border-color: #DCDCDC;">
                <c:forEach items="${questions}" var="question">
                    <c:choose>
                        <c:when test="${question.category == 10}">
                            <tr class="table" style="background-color: #F0F0F0;">
                                <td class="text-center">${question.no}</td>
                                <td class="text-center">
                                <span class="badge" style="background-color: #F87217; color: white;">공지</span></td>
                                <td><a href="/qna/${question.no}"
                                    class="text-dark qna-link notice-title">${question.title}</a></td>
                                <td class="text-center">${question.id}</td>
                                <td class="text-center">${question.createDate}</td>
                                <td class="text-center">${question.views}</td>
                            </tr>
                        </c:when>
                        <c:otherwise>
                            <tr>
                                <td class="text-center">${question.no}</td>
                                <td class="text-center"><c:choose>
                                        <c:when test="${question.category == 20}">
                                            <span class="badge"
                                                style="background-color: #024089; color: white;">답변완료</span>
                                        </c:when>
                                        <c:when test="${question.category == 30}">
                                            <span class="badge"
                                                style="background-color: #E0EAF5; color: black;">답변대기</span>
                                        </c:when>
                                    </c:choose></td>
                                <td><a href="/qna/${question.no}"
                                    class="text-dark qna-link">${question.title}</a></td>
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
        <div class="d-flex justify-content-end">
            <a href="/qna/save" class="btn btn-outline-primary my-1"
                role="button">글쓰기</a>
        </div>
        <!-- 페이징 -->
        <div class="my-2">
            <nav aria-label="Page navigation">
                <ul class="pagination justify-content-center" id="pagination">
                    <!-- 첫 페이지 버튼 -->
                    <li class="page-item ${page <= 1 ? 'disabled' : ''}">
                    <a class="page-link" href="${page > 1 ? '/qna?page=1&size=10' : '#'}"
                        aria-label="First" style="background-color: #F0F0F0;"> 
                        <span aria-hidden="true">&laquo;&laquo;</span>
                    </a></li>
                    <!-- 이전 페이지 버튼 -->
                    <li class="page-item ${page <= 1 ? 'disabled' : ''}">
                    <a class="page-link"
                        href="${page > 1 ? '/qna?page='.concat(page - 1).concat('&size=10') : '#'}"
                        aria-label="Previous" style="background-color: #F0F0F0;"> 
                        <span aria-hidden="true">&laquo;</span>
                    </a></li>
                    <!-- 페이지 번호 -->
                    <c:forEach begin="1" end="${totalPages}" var="pageNum">
                        <li class="page-item ${page == pageNum ? 'active' : ''}">
                        <a class="page-link" href="/qna?page=${pageNum}&size=10">${pageNum}</a>
                        </li>
                    </c:forEach>
                    <!-- 다음 페이지 버튼 -->
                    <li class="page-item ${page >= totalPages ? 'disabled' : ''}">
                        <a class="page-link"
                        href="${page < totalPages ? '/qna?page='.concat(page + 1).concat('&size=10') : '#'}"
                        aria-label="Next" style="background-color: #F0F0F0;"> 
                        <span aria-hidden="true">&raquo;</span>
                    </a>
                    </li>
                    <!-- 마지막 페이지 버튼 -->
                    <li class="page-item ${page >= totalPages ? 'disabled' : ''}">
                        <a class="page-link"
                        href="${page < totalPages ? '/qna?page='.concat(totalPages).concat('&size=10') : '#'}"
                        aria-label="Last" style="background-color: #F0F0F0;"> 
                        <span aria-hidden="true">&raquo;&raquo;</span>
                    </a>
                    </li>
                </ul>
                <!-- 마지막 페이지 안내 메시지 -->
                <c:if test="${page == totalPages}">
                    <p class="text-center">마지막 페이지입니다.</p>
                </c:if>
            </nav>
        </div>
        <!-- // 페이징 end -->
    </div>
    <script>
        const pagination = document.getElementById("pagination");
        const pageButtons = pagination.querySelectorAll(".page-item");
    
        pageButtons.forEach(button => {
            button.addEventListener("click", () => {
                if (!button.classList.contains("disabled")) {
                    pageButtons.forEach(btn => btn.classList.remove("active"));
                    button.classList.add("active");
                }
            });
        });
    </script>
</body>
<footer class="py-3 my-4 mt-auto">
  <ul class="nav justify-content-center border-bottom pb-3 mb-3">
  </ul>
  <p class="text-center text-body-secondary">&copy; 2023 F1 RoadScanner Project, All rights reserved.</p>
</footer>
</html>