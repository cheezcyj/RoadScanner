<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
<style>
    /* 메뉴에 마우스 올렸을 때 밑줄 적용 */
    .nav-item:hover .nav-link {
        text-decoration: underline;
    }
</style>
    <title>로드스캐너</title>
</head>
<body>
    <header>
        <nav class="navbar navbar-expand-lg navbar-light" style="background-color: #e3f2fd;">
            <div class="container">
                <a class="navbar-brand" href="/qna">ROADSCANNER</a>
                <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav" aria-controls="navbarNav" aria-expanded="false" aria-label="Toggle navigation">
                    <span class="navbar-toggler-icon"></span>
                </button>
                <div class="collapse navbar-collapse" id="navbarNav">
                    <ul class="navbar-nav ml-auto">
                        <li class="nav-item active">
                            <a class="nav-link" href="/qna/main">홈 <span class="sr-only">(current)</span></a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link" href="/qna/listtest">(테스트)게시판</a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link" href="/qna/list">게시판</a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link" href="/qna/listadmin">(관리자)게시판</a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link" href="/qna/write">게시글 쓰기</a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link" href="/qna/writeadmin">(관리자)게시글 쓰기</a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link" href="#">파일 업로드하기</a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link" href="#">로그인</a>
                        </li>
                    </ul>
                </div>
            </div>
        </nav>
    </header>
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

    <script>
      // 햄버거 버튼과 네비게이션 바 요소 가져오기
      const navbarToggler = document.querySelector(".navbar-toggler");
      const navbarCollapse = document.querySelector(".navbar-collapse");
    
      // 햄버거 버튼 클릭 시 동작 정의
      navbarToggler.addEventListener("click", () => {
          if (navbarCollapse.classList.contains("show")) {
              // 네비게이션 바가 열려있을 때 누르면 닫힘
              navbarCollapse.classList.remove("show");
          } else {
              // 네비게이션 바가 닫혀있을 때 누르면 열림
              navbarCollapse.classList.add("show");
          }
      });
    </script>

    <footer class="text-dark text-center py-3 position-absolute w-100" style="bottom: 0; background-color: #e3f2fd;">
        &copy; 2023 ROADSCANNER. All Rights Reserved.
    </footer>

	<!-- 부트스트랩 JS 및 Popper.js 추가 -->
	<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js"></script>
	
	<script>
	document.addEventListener("DOMContentLoaded", () => {
	    const navbarToggler = document.querySelector(".navbar-toggler");
	    const navbarCollapse = document.querySelector(".navbar-collapse");
	
	    navbarToggler.addEventListener("click", () => {
	        if (navbarCollapse.classList.contains("show")) {
	            navbarCollapse.classList.remove("show");
	        } 
	    });
	});
	</script>

</body>
</html>