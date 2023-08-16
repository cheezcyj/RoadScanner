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

    <!-- 답변 내용 -->
    <c:if test="${question.category != 10}">
        <div class="card mb-5">
            <div class="card-body" id="answer-section">
                <c:choose>
                    <c:when test="${answer == null}">
                        <!-- 답변이 없을 경우 답변 등록 폼을 표시 -->
                        <div class="mb-3">
                            <form id="answer-form">
                                <label for="id" class="form-label">작성자:</label>
                                <!-- 나중에 관리자 session값 주고 readonly로 변경 예정 -->
                                <input type="text" id="id" class="form-control">

                                <textarea class="form-control" id="answer-content" rows="5" placeholder="답변을 입력하세요"></textarea>
                                <a href="#" role="button" class="btn btn-secondary">취소</a>
                                <button type="submit" id="btn-answer-save" class="btn btn-primary" value="저장">등록</button>
                            </form>
                        </div>
                    </c:when>

                    <c:otherwise>
                    <!-- 답변이 있는 경우 답변 내용을 표시 -->
                        <div class="mb-3" id="answer-detail">
                            <p class="card-text">작성자: ${answer.id} </p>   <!-- 나중에 관리자 session 가져올 예정 -->
                            <p class="card-text">작성일: ${answer.createDate}</p>
                            <c:if test="${answer.updateDate != null}">
                                <p class="card-text">
                                    최종 수정일: ${answer.updateDate}
                                </p>
                            </c:if>
                            <p class="card-text">내용: ${answer.content}</p>
                            <button type="button" id="btn-answer-update-form" class="btn btn-light">수정</button>
                            <button type="button" id="btn-answer-delete" class="btn btn-light">삭제</button>
                        </div>
                    </c:otherwise>
                </c:choose>

                <!-- 답변 수정 버튼을 누를 경우 답변 수정 form 표시 -->
                <div class="mb-3">
                    <form id="answer-update-form" style="display: none;">
                        <label for="update-id" class="form-label">작성자:</label>
                        <input type="text" id="update-id" class="form-control" readonly="readonly" value="${answer.id}">
                        <textarea class="form-control" id="answer-update-content" rows="5">${answer.content}</textarea>
                        <a href="#" role="button" class="btn btn-secondary">취소</a>
                        <button type="submit" id="btn-answer-updated" class="btn btn-primary" value="수정">완료</button>
                    </form>
                </div>
            </div>
        </div>
    </c:if>
</div>

<!-- 부트스트랩 JS 및 Popper.js 추가 -->
<script src="https://code.jquery.com/jquery-3.7.0.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="/resources/js/qna.js"></script></body>
</html>