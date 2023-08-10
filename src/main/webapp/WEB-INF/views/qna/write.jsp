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
<title>게시판 글쓰기</title>
<!-- Bootstrap CSS -->
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css" rel="stylesheet">
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

    <div class="container my-4">
    
        <form>
            <h2 class="mb-4">Q&A 게시판</h2>

            <div class="mb-3 row">
                <label for="postTitle" class="col-sm-2 col-form-label">제목</label>
                <div class="col-sm-10">
                    <input type="text" class="form-control" id="postTitle" value="${question.title}">
                </div>
            </div>
            
            <div class="mb-3 row">
                <label for="author" class="col-sm-2 col-form-label">작성자</label>
                <div class="col-sm-10">
                    <input type="text" class="form-control" id="author" value="${question.id}" readonly>
                </div>
            </div>
            
            <div class="mb-3 row">
                <label for="attachment" class="col-sm-2 col-form-label">첨부파일</label>
                <div class="col-sm-10">
                    <input type="file" class="form-control" id="attachment">
                </div>
            </div>
            
            <div class="mb-3 row">
                <label for="content" class="col-sm-2 col-form-label">내용</label>
                <div class="col-sm-10">
                    <textarea class="form-control" id="content" rows="10">${question.content}</textarea>
                </div>
            </div>
            
            <div class="text-center">
                <button type="button" class="btn btn-secondary me-2" onclick="location.href='/qna/list';">취소</button>
                <button type="submit" class="btn btn-primary me-2">작성완료</button>
            </div>
        </form>
    </div>

	<!-- JavaScript -->
	<script>
	    function validateForm() {
	        var postTitle = document.getElementById("postTitle").value;
	        var content = document.getElementById("content").value;
	        
	        if (postTitle.trim() === "" || content.trim() === "") {
	            alert("제목과 내용을 입력해주세요.");
	            return false; // 폼 제출 중지
	        }
	        return true; // 폼 제출 진행
	    }
	</script>
	
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