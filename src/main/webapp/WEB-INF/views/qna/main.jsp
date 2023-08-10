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

<!-- 부트스트랩 CSS 추가 -->
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body style="position: relative; min-height: 100vh;">
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

    <main class="container mt-5" style="margin-bottom: 100px;">
        <h1>환영합니다!</h1>
        <p>ROADSCANNER 메인페이지입니다.</p>
        <br>목록은 네비게이션바에 있습니다.</br>
    </main>

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