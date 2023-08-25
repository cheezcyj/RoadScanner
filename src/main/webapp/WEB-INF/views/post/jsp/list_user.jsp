<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<!-- Bootstrap Icons -->
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.3/font/bootstrap-icons.css">
<!-- CSS only -->
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-rbsA2VBKQhggwzxH7pPCaAqO46MgnOM80zW1RWuH61DGLwZJEdK2Kadq2F9CUG65" crossorigin="anonymous">
<!-- JavaScript Bundle with Popper -->
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js" integrity="sha384-kenU1KFdBIe4zVF0s0G1M5b4hcpxyD9F7jL+jjXkk+Q2h455rYXK/7HAuoJl+0I4" crossorigin="anonymous"></script>
<script src="${CP}/resources/js/jquery-3.7.0.js"></script>
<script src="${CP}/resources/js/util.js"></script>
<title>게시판 목록</title>
</head>
<body>
  <header class="bg-light text-center p-4">
      <h1>로드스캐너</h1>
  </header>
  
  <div class="container my-4">
  
    <!-- 제목 -->
    <div class="page-header">
     <h2 class="mb-4">Q&A 게시판</h2>
    </div>
    <!-- 제목 end --------------------------------------------------------------->
    
    <!-- 검색 form -->
    <form name="searchFrm">
      <div class="row g-1 d-flex justify-content-end mt-0 mb-3">  
        <!-- 게시물 목록 수 선택 셀렉트 박스 -->
        <div class="col-auto">
          <select class="form-select" id="itemsPerPage">
            <option value="10">10개씩 보기</option>
            <option value="30">30개씩 보기</option>
            <option value="50">50개씩 보기</option>
            <option value="100">100개씩 보기</option>
            <option value="200">200개씩 보기</option>
          </select>
        </div>
        <!-- 분류 셀렉트 박스 -->
        <div class="col-auto">
          <select class="form-select" name="category" id="category">
            <option value="">--분류--</option>
            <option value="공지">공지</option>
            <option value="답변대기">답변대기</option>
            <option value="답변완료">답변완료</option>
          </select>
        </div>
        <!-- 검색 셀렉트 박스 -->
        <div class="col-auto">
          <select class="form-select" name="searchDiv" id="searchDiv">
            <option value="">--전체--</option>
            <option value="제목">제목</option>
            <option value="내용">내용</option>
            <option value="제목+내용">제목+내용</option>
          </select>
        </div>
        
        <div class="col-auto">
            <div class="input-group">
                <span class="input-group-text" id="basic-addon1">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-search" viewBox="0 0 16 16" >
                    <path d="M11.742 10.344a6.5 6.5 0 1 0-1.397 1.398h-.001c.03.04.062.078.098.115l3.85 3.85a1 1 0 0 0 1.415-1.414l-3.85-3.85a1.007 1.007 0 0 0-.115-.1zM12 6.5a5.5 5.5 0 1 1-11 0 5.5 5.5 0 0 1 11 0z"></path>
                    </svg>
                </span>
                <input type="text" class="form-control" id="searchWord" placeholder="검색어를 입력하세요.">
            </div>
        </div>
        
        <div class="col-auto">   
          <a class="btn btn-primary" href="/board/write">글쓰기</a>
        </div>
      </div>
    </form>
    <!--// 검색 form end -------------------------------------------------------->
    
    <!-- 게시판 테이블 -->
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
       <script>
          // 공지 게시물 먼저 출력
          for (let i = 0; i < question.length; i++) {
            if (question[i].category === '공지') {
              document.write(`
                <tr>
                  <td class="text-center col-sm-1 col-md-1 col-lg-1">${question[i].no}</td>
                  <td class="text-center col-sm-2 col-md-2 col-lg-1">${question[i].category}</td>
                  <td class="text-center col-sm-5 col-md-5 col-lg-5"><a href="#" style="text-decoration-line: none;">${question[i].title}</a></td>
                  <td class="text-center col-sm-1 col-md-2 col-lg-2">${question[i].id}</td>
                  <td class="text-center col-sm-2 col-md-1 col-lg-2">${question[i].createDate}</td>
                  <td class="text-center col-sm-1 col-md-1 col-lg-1">${question[i].views}</td>
                  <td style="display:none;">${question[i].hiddenValue}</td>
                </tr>
              `);
            }
          }
          
          // 나머지 게시물 출력
          for (let i = 0; i < question.length; i++) {
            if (question[i].category !== '공지') {
              document.write(`
                <tr>
                  <td class="text-center col-sm-1 col-md-1 col-lg-1">${question[i].no}</td>
                  <td class="text-center col-sm-2 col-md-2 col-lg-1">${question[i].category}</td>
                  <td class="text-center col-sm-5 col-md-5 col-lg-5"><a href="#" style="text-decoration-line: none;">${question[i].title}</a></td>
                  <td class="text-center col-sm-1 col-md-2 col-lg-2">${question[i].id}</td>
                  <td class="text-center col-sm-2 col-md-1 col-lg-2">${question[i].createDate}</td>
                  <td class="text-center col-sm-1 col-md-1 col-lg-1">${question[i].views}</td>
                  <td style="display:none;">${question[i].hiddenValue}</td>
                </tr>
              `);
            }
          }
        </script>
       
        <!-- c:forEach를 사용하여 게시글 목록 생성 -->
        <c:forEach items="${question}" var="question">
          <tr>
            <td class="text-center col-sm-1 col-md-1 col-lg-1">${question.no}</td>
            <td class="text-center col-sm-2 col-md-2 col-lg-1">${question.category}</td>
            <td class="text-center col-sm-5 col-md-5 col-lg-5"><a href="#" style="text-decoration-line: none;">${question.title}</a></td>
            <td class="text-center col-sm-1 col-md-2 col-lg-2">${question.id}</td>
            <td class="text-center col-sm-2 col-md-1 col-lg-2">${question.createDate}</td>
            <td class="text-center col-sm-1 col-md-1 col-lg-1">${question.views}</td>
            <td style="display:none;">1</td>
          </tr>
        </c:forEach>
        
      </tbody>
    </table>
    <!-- // 게시판 테이블 끝 -------------------------------------------------------->
       
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
  <!-- container end ---------------------------------------------------------->
  
<!-- 부트스트랩 JS 및 Popper.js 추가 -->
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="/resources/js/qna.js"></script>
  
</body>
</html>