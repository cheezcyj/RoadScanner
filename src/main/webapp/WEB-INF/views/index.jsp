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
    <a class="navbar-brand" href="/">RoadScanner</a>
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
<body>
    <nav class="navbar navbar-expand-lg navbar-light" style="background-color: #e3f2fd;">
        <div class="container">
            <a class="navbar-brand" href="/">RoadScanner</a>
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
                        <a class="nav-link" href="/qna/{no}">(테스트)글상세보기</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="/qna/save">(테스트)글쓰기</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="/qna/update/{no}">(테스트)글수정</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="/qna/list">게시판</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="/qna/detail">글상세보기</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="/qna/write">글쓰기</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="/qna/mod">글수정</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="/qna/listadmin">(관리자)게시판</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="/qna/writeadmin">(관리자)글쓰기</a>
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
<div>
    <img src="https://r2.jjalbot.com/2023/03/EZSzTAXQJR.jpeg" alt="포기하면편해">
    <h2>우리 팀이 해야 할 일: 추가 사항있으면 작성해주세요. + 공유!</h2>
    <ul>
        <li><h3>게시판 CRUD</h3></strong>
            <ul>
                <li><a href="/qna">전체 게시글 출력 R</a></li>
                <li><a href="/qna/save">게시글 작성 C</a></li>
            </ul>
        </li>
        <li><h4>etc</h4>
            <ul>
                <li><a href="#">페이징</a></li>
                <li><a href="#">게시물 검색 - 어떤 기준으로 할 지 정해야함</a></li>
            </ul>
        </li>
    </ul>
</div>

<div>
    <h2>댓글</h2>
    <ul>
        <li>댓글 CRUD
            <ul>
                <li><a href="#">댓글 등록</a></li>
                <li><a href="#"></a></li>
                <li><a href="#"></a></li>
            </ul>
        </li>
    </ul>
</div>

<h3>다른팀과의 연동</h3>
<div>
    <ul>
        <li>너무많아서 생략...</li>
    </ul>
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