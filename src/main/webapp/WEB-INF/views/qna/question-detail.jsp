<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <title>로드스캐너</title>

    <!-- 부트스트랩 CSS 추가 -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>

<div class="container mt-5">
    <h1 class="mb-4">Q&A 게시판</h1>
    <div class="card">
        <input type="hidden" id="no" value="${question.no}">
        <div class="card-header">
            <h2 class="card-title">${question.title}</h2>
        </div>
        <div class="card-body">
            <p class="card-text">작성자: ${question.id}</p>
            <p class="card-text">
                조회수: ${question.views}
            </p>
            <p class="card-text">
                작성일: ${question.createDate}
            </p>
            <c:if test="${question.updateDate != null}">
                <p class="card-text">
                    최종 수정일: ${question.updateDate}
                </p>
            </c:if>
            <p class="card-text">
                내용: ${question.content}
            </p>
            <div class="mt-4">
                <a href="/qna/update/${question.no}" class="btn btn-primary">수정</a>
                <button type="button" id="btn-delete" class="btn btn-danger">삭제</button>
            </div>
        </div>
    </div>


    <!-- 답변 작성 폼 -->
    <div class="card">
        <form id="answer-form">
            <div class="mb-3">
                <textarea class="form-control" id="answer-content" rows="5" placeholder="답변을 입력하세요"></textarea>
            </div>
            <a href="#" role="button" class="btn btn-secondary">취소</a>
            <button type="submit" id="btn-answer-save" class="btn btn-primary" value="저장">등록</button>
        </form>
    </div>

    <!-- 답변 내용 -->
    <div id="answer-section">
    </div>

</div>

<!-- 부트스트랩 JS 및 Popper.js 추가 -->
<script src="https://code.jquery.com/jquery-3.7.0.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="/resources/js/qna.js"></script></body>
</html>