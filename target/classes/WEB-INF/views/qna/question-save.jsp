<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <title>로드스캐너 - 게시판 등록</title>
</head>
<body>
<form class="container mt-5" id="question-form">
    <div class="mb-3">
        <label for="category" class="form-label">카테고리:</label>
        <select id="category" name="category" class="form-select">
            <option value="30" selected>답변 대기</option>
            <option value="20">답변 완료</option>
            <option value="10">공지</option>
        </select>
    </div>
    <div class="mb-3">
        <label for="id" class="form-label">작성자:</label>
        <input type="text" id="id" class="form-control">
    </div>
    <div class="mb-3">
        <label for="title" class="form-label">제목:</label>
        <input type="text" id="title" class="form-control">
    </div>
    <div class="mb-3">
        <label for="idx" class="form-label">첨부파일:</label>
        <input type="text" id="idx" class="form-control">
    </div>
    <div class="mb-3">
        <label for="content" class="form-label">내용:</label>
        <textarea id="content" class="form-control" placeholder="내용을 입력하세요"></textarea>
    </div>
    <a href="/qna" role="button" class="btn btn-secondary">취소</a>
    <button type="submit" id="btn-save" class="btn btn-primary" value="저장">등록</button>
</form>
<script src="https://code.jquery.com/jquery-3.7.0.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="/resources/js/qna.js"></script>
</body>
</html>