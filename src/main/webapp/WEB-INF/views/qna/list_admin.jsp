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
    <title>관리자 - 게시판 목록</title>
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
<body>
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
  <div class="container my-4">
    <div class="page-header">
      <h2 class="mb-4">관리자 전용 게시판</h2>
    </div>
    
    <form name="searchFrm">
      <div class="row g-1 d-flex justify-content-end mt-0 mb-3">  
        <div class="col-auto">
          <select class="form-select" name="category" id="category">
            <option value="">--분류--</option>
            <option value="공지">공지</option>
          </select>
        </div>
        <div class="col-auto">
          <select class="form-select" name="searchDiv" id="searchDiv">
            <option value="">--전체--</option>
            <option value="title">제목</option>
            <option value="content">내용</option>
            <option value="titleAndContent">제목+내용</option>
          </select>
        </div>
        
        <div class="col-auto">
            <div class="input-group">
                <span class="input-group-text" id="basic-addon1">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-search" viewBox="0 0 16 16">
                    <path d="M11.742 10.344a6.5 6.5 0 1 0-1.397 1.398h-.001c.03.04.062.078.098.115l3.85 3.85a1 1 0 0 0 1.415-1.414l-3.85-3.85a1.007 1.007 0 0 0-.115-.1zM12 6.5a5.5 5.5 0 1 1-11 0 5.5 5.5 0 0 1 11 0z"></path>
                    </svg>
                </span>
                <input type="text" class="form-control" id="searchWord" placeholder="검색어를 입력하세요.">
            </div>
        </div>
        
        <div class="col-auto">   
          <a class="btn btn-primary" href="/qna/writeadmin">공지글쓰기</a>
        </div>
      </div>
    </form>
    
    <table class="table table-hover" id="boardTable">
      <thead class="table-light">
        <tr>
          <th class="text-center">번호</th>
          <th class="text-center">분류</th>
          <th class="text-center">제목</th>
          <th class="text-center">작성자</th>
          <th class="text-center">작성일</th>
          <th class="text-center">조회수</th>
        </tr>
      </thead>
      <tbody>
        <c:forEach items="${questions}" var="q">
          <tr class="${q.category eq '답변대기' ? 'table-secondary' : 'table-light'}">
            <td class="text-center col-sm-1 col-md-1 col-lg-1" id="postNum">${q.no}</td>                                                                       
            <td class="text-center col-sm-2 col-md-2 col-lg-1" id="category">${q.category}</td>                                                          
            <td class="text-center col-sm-5 col-md-5 col-lg-5" id="postTitle"><a href="#" style="text-decoration-line: none;">${q.title}</a></td>
            <td class="text-center col-sm-1 col-md-2 col-lg-2" id="author">${q.id}</td>                                                                        
            <td class="text-center col-sm-2 col-md-1 col-lg-2" id="postDate">${q.createDate}</td>                                                              
            <td class="text-center col-sm-1 col-md-1 col-lg-1" id="readCnt">${q.views}</td>                                                                    
            <td style="display:none;">1</td>
          </tr>
        </c:forEach>
      </tbody>
    </table>
    
        <!-- pagination -->
        <nav aria-label="Page navigation">
          <ul class="pagination justify-content-center">
            <li class="page-item">
              <a class="page-link" href="#" aria-label="First">
                <span aria-hidden="true">&laquo;&laquo;</span>
              </a>
            </li>
            <li class="page-item">
              <a class="page-link" href="#" aria-label="Previous">
                <span aria-hidden="true">&laquo;</span>
              </a>
            </li>
            <li class="page-item active"><a class="page-link" href="#">1</a></li>
            
            <!-- JavaScript로 페이지 숫자 동적 생성 -->
            <script>
              // 서버에서 가져온 총 데이터 개수
              const totalDataCount = /* 서버에서 가져온 총 데이터 개수 */;
              const itemsPerPage = 10; // 페이지당 아이템 개수
              const totalPages = Math.ceil(totalDataCount / itemsPerPage); // 총 페이지 개수
              const currentPage = 1; // 현재 페이지 번호
              
              for (let i = 2; i <= totalPages; i++) {
                if (i === currentPage) {
                  document.write(`
                    <li class="page-item active"><a class="page-link" href="#">${i}</a></li>
                  `);
                } else {
                  document.write(`
                    <li class="page-item"><a class="page-link" href="#">${i}</a></li>
                  `);
                }
              }
            </script>
            
            <li class="page-item">
              <a class="page-link" href="#" aria-label="Next">
                <span aria-hidden="true">&raquo;</span>
              </a>
            </li>
            <li class="page-item">
              <a class="page-link" href="#" aria-label="Last">
                <span aria-hidden="true">&raquo;&raquo;</span>
              </a>
            </li>
          </ul>
        </nav>
        <!-- pagination end -------------------------------------------------------->
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