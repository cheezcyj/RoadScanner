<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css" integrity="sha384-rbsA2VBKQhggwzxH7pPCaAqO46MgnOM80zW1RWuH61DGLwZJEdK2Kadq2F9CUG65" crossorigin="anonymous">
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js" integrity="sha384-kenU1KFdBIe4zVF0s0G1M5b4hcpxyD9F7jL+jjXkk+Q2h455rYXK/7HAuoJl+0I4" crossorigin="anonymous"></script>
<script src="${pageContext.request.contextPath}/resources/js/jquery-3.7.0.js"></script>
<script src="${pageContext.request.contextPath}/resources/js/util.js"></script>
<title>관리자 - 게시판 목록</title>
</head>
<body>
  <header class="bg-light text-center p-4">
      <h1>로드스캐너</h1>
  </header>
  
  <div class="container my-4">
    <div class="page-header">
      <h2 class="mb-4">관리자 전용 게시판</h2>
    </div>
    
    <form name="searchFrm">
      <div class="row g-1 d-flex justify-content-end mt-0 mb-3">  
        <div class="col-auto">
          <select class="form-select" name="category" id="category">
            <option value="">--분류--</option>
            <option value="답변대기">답변대기</option>
            <option value="답변완료">답변완료</option>
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
          <a class="btn btn-primary" href="/board/write">글쓰기</a>
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
    
    <nav aria-label="Page navigation">
      <ul class="pagination justify-content-center">
        <li class="page-item">
          <a class="page-link" href="#" aria-label="Previous">
            <span aria-hidden="true">&laquo;</span>
          </a>
        </li>
        <li class="page-item"><a class="page-link" href="#">1</a></li>
        <li class="page-item"><a class="page-link" href="#">2</a></li>
        <li class="page-item"><a class="page-link" href="#">3</a></li>
        <li class="page-item">
          <a class="page-link" href="#" aria-label="Next">
            <span aria-hidden="true">&raquo;</span>
          </a>
        </li>
      </ul>
    </nav>
  </div>
</body>
</html>
