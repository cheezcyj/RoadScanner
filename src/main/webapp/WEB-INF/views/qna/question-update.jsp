<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css" rel="stylesheet">
  <link href="/resources/css/qna.css" rel="stylesheet">
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
  <title>로드스캐너 - 게시판 수정</title>
</head>
<body>
     <form class="container mt-4" id="question-edit-form">
        <h1 class="mb-4"><a href="/qna" class="qna-title-link">Q&A 게시판</a></h1>
        <!-- 이 부분에 히든 필드 추가 -->
        <input type="hidden" id="no" value="${question.no}">

        <div class="mb-2 row align-items-center" style="display: none;">
            <label for="category" class="form-label col-auto pe-1 m-0">답변 상태</label>
            <div class="col">
                <input type="text" id="category" class="form-control" value="${question.category}" readonly>
            </div>
        </div>

        <div class="row" style="display: none;">
            <label for="id" class="col-sm-2 col-form-label">작성자</label>
            <div class="col-sm-10">
                <input type="text" class="form-control" id="id" value="${user.id}" readonly>
            </div>
        </div>

        <div class="mb-3 row">
            <div class="col">
                <input type="text" class="form-control" id="title" value="${question.title}" placeholder="제목을 입력하세요.">
            </div>
        </div>

        <div class="d-flex mb-3 row align-items-center">
            <div class="col">
                <input type="file" id="attachFile" name="attachFile" class="form-control" accept="image/*">
<%--                <input type="text" class="form-control" value="${question.originalFilename}" placeholder="원본파일명">--%>
            </div>
        </div>

        <div class="mb-3 row">
            <div class="col">
                <textarea class="form-control" id="content" rows="10" placeholder="내용을 입력하세요.">${question.content}</textarea>
            </div>
        </div>

        <div class="text-center">
            <a href="/qna/${question.no}" role="button" class="btn btn-secondary">취소</a>
            <button type="button" id="btn-update" class="btn" value="수정">수정</button>
        </div>
    </form>
<script src="https://code.jquery.com/jquery-3.7.0.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="/resources/js/qna.js"></script>
</body>
</html>