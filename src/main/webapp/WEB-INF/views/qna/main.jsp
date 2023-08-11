<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
<!-- 부트스트랩 CSS 추가 -->
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css" rel="stylesheet">
<style>
    /* 메뉴에 마우스 올렸을 때 밑줄 적용 */
    .nav-item:hover .nav-link {
        text-decoration: underline;
    }
</style>
    <title>로드스캐너</title>
</head>
<nav class="navbar navbar-expand-md mb-4" style="background-color: white;">
  <div class="container-fluid">
    <a class="navbar-brand" href="/qna">RoadScanner</a>
    <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarCollapse" aria-controls="navbarCollapse" aria-expanded="false" aria-label="Toggle navigation">
      <span class="navbar-toggler-icon"></span>
    </button>
    <div class="collapse navbar-collapse" id="navbarCollapse">
      <ul class="navbar-nav me-auto mb-2 mb-md-0">
        <li class="nav-item">
          <a class="nav-link active" aria-current="page" href="#">Home</a>
        </li>
        <li class="nav-item">
          <a class="nav-link" href="#">Link</a>
        </li>
        <li class="nav-item">
          <a class="nav-link disabled" aria-disabled="true">Disabled</a>
        </li>
      </ul>
      <form class="d-flex" role="search">
        <!-- 로그인 세션 X -->
        <c:if test="${user ne null}">
          <button type="button" id="login" onclick="location.href='${CP}/login'" class="btn btn-outline-primary me-2">Login</button>
        </c:if>
        
        <!-- 로그인 세션 O -->
        <c:if test="${user eq null}">
          <button type="button" class="btn btn-outline-primary me-2" onclick="location.href='${CP}/mypage'">MyPage</button>
          <button type="button" class="btn btn-outline-primary me-2" onclick="location.href='${CP}/logout'">LogOut</button>
        </c:if>
          <button type="button" onclick="location.href='${CP}/registerpage'" class="btn btn-outline-primary" style="margin-right: 50px;">Sign-up</button>
      </form>
    </div>
  </div>
</nav>
<body style="position: relative; min-height: 100vh;">
    <nav class="navbar navbar-expand-lg navbar-light" style="background-color: #e3f2fd;">
        <div class="container">
            <a class="navbar-brand" href="/qna">RoadScanner</a>
            <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav" aria-controls="navbarNav" aria-expanded="false" aria-label="Toggle navigation">
                <span class="navbar-toggler-icon"></span>
            </button>
            <div class="collapse navbar-collapse" id="navbarNav">
                <ul class="navbar-nav ml-auto">
                    <li class="nav-item active">
                        <a class="nav-link" href="/qna/main">홈</a>
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
                        <a class="nav-link" href="/qna/detail">글상세보기</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="/qna/mod">글수정</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="/qna/modadmin">(관리자)글수정</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="#">파일 업로드하기</a>
                    </li>
                </ul>
            </div>
        </div>
    </nav>
    <main class="container mt-5" style="margin-bottom: 100px;">
        <h1>환영합니다!</h1>
        <p>ROADSCANNER 메인페이지입니다.
        <br><br>(목록은 네비게이션바에 있습니다)</p>
    </main>

    <footer class="py-3 my-4 mt-auto">
      <ul class="nav justify-content-center border-bottom pb-3 mb-3">
      </ul>
      <p class="text-center text-body-secondary">&copy; 2023 F1 RoadScanner Project, All rights reserved.</p>
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