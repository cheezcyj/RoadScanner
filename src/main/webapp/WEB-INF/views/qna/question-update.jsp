<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css" rel="stylesheet">
<title>로드스캐너 - 게시판 수정</title>
</head>
<body>
<form class="container mt-5" id="question-edit-form">
    <!-- 이 부분에 히든 필드 추가 -->
    <input type="hidden" id="no" value="${question.no}">

  <div class="mb-3">
    <label for="id" class="form-label">답변상태:</label>
    <input type="text" id="category" class="form-control" value="${question.category}" readonly>
  </div>

  <div class="mb-3">
    <label for="id" class="form-label">작성자:</label>
    <input type="text" id="id" class="form-control" value="${question.id}" readonly>
  </div>
  <div class="mb-3">
    <label for="title" class="form-label">제목:</label>
    <input type="text" id="title" class="form-control" value="${question.title}">
  </div>
  <div class="mb-3">
    <label for="idx" class="form-label">첨부파일:</label>
    <input type="text" id="idx" class="form-control" value="${question.idx}">
  </div>
  <div class="mb-3">
    <label for="content" class="form-label">내용:</label>
    <textarea id="content" class="form-control">${question.content}</textarea>
  </div>
  <a href="/qna" role="button" class="btn btn-secondary">취소</a>
  <button type="button" id="btn-update" class="btn btn-primary" value="수정">수정</button>
</form>
<script src="https://code.jquery.com/jquery-3.7.0.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="/resources/js/qna.js"></script>
</body>
</html>
