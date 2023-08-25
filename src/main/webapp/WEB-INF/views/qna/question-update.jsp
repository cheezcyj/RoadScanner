<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<html>
<head>
<title>로드스캐너 - 게시판 수정</title>
<meta charset="UTF-8">
<meta name="viewport"
    content="width=device-width, initial-scale=1, shrink-to-fit=no">
<!-- 부트스트랩 CSS 추가 -->
<link
    href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css"
    rel="stylesheet">
<link  href="${c}/resources/css/membership-style.css" rel="stylesheet">
<link  href="${c}/resources/css/bootstrap/bootstrap.min.css" rel="stylesheet"  crossorigin="anonymous">
<script src="${c}/resources/js/bootstrap/bootstrap.bundle.min.js"  crossorigin="anonymous"></script>
</head>
<nav class="navbar navbar-expand-md mb-4" style="background-color: white;">
  <div class="container-fluid">
    <a class="navbar-brand" href="#">RoadScanner</a>
    <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarCollapse" aria-controls="navbarCollapse" aria-expanded="false" aria-label="Toggle navigation">
      <span class="navbar-toggler-icon"></span>
    </button>
    <div class="collapse navbar-collapse" id="navbarCollapse">
      <ul class="navbar-nav me-auto mb-2 mb-md-0">
        <c:if test="${user ne null}">
          <li class="nav-item">
            <a class="nav-link" href="${c}/main/preUpload">사진 업로드</a>
          </li>
          <li class="nav-item">
            <a class="nav-link" href="${c}/qna">게시판</a>
          </li>
        </c:if>
        <c:if test="${user.grade == 2}">
        <li class="nav-item dropdown">
          <input type="hidden" id="nekeyword" name="nekeyword" value ="${user.id}">
          <a class="nav-link dropdown-toggle" href="#" data-bs-toggle="dropdown" aria-expanded="false">관리자 기능</a>
          <ul class="dropdown-menu">
            <li><a class="dropdown-item" href="${c}/admin">List</a></li>
            <li><a class="dropdown-item" href="#">Upload</a></li>
            <li><a class="dropdown-item" href="#">None</a></li>
          </ul>
        </li>
        </c:if>
      </ul>
      <form class="d-flex" role="search">
        <!-- 로그인 세션 X -->
        <c:if test="${user eq null}">
          <button type="button" id="login" onclick="location.href='${c}/login'" class="btn btn-outline-primary me-2">Login</button>
          <button type="button" onclick="location.href='${c}/registerpage'" class="btn btn-outline-primary" style="margin-right: 50px;">Sign-up</button>
        </c:if>
        <!-- 로그인 세션 O -->
        <c:if test="${user ne null}">
          <button type="button" class="btn btn-outline-primary me-2" onclick="location.href='${c}/mypage'">MyPage</button>
          <button type="button" class="btn btn-outline-primary" onclick="location.href='${c}/logout'" style="margin-right: 50px;">LogOut</button>
        </c:if>
      </form>
    </div>
  </div>
</nav>
<body>
    <form class="container mt-4" id="question-edit-form">
        <h1 class="mb-4">Q&A 게시판</h1>
        <!-- 이 부분에 히든 필드 추가 -->
        <input type="hidden" id="no" value="${question.no}">

        <div class="mb-2 row align-items-center" style="display: none;">
            <label for="category" class="form-label col-auto pe-1 m-0">답변 상태</label>
            <div class="col">
                <input type="text" id="category" class="form-control"
                    value="${question.category}" readonly>
            </div>
        </div>

        <div class="row" style="display: none;">
            <label for="id" class="col-sm-2 col-form-label">작성자</label>
            <div class="col-sm-10">
                <input type="text" class="form-control" id="id" value="${userId}" readonly>
            </div>
        </div>

        <div class="mb-2 row">
            <div class="col">
                <input type="text" class="form-control" id="title"
                    value="${question.title}" placeholder="제목을 입력하세요.">
            </div>
        </div>

        <div class="mb-2 row align-items-center">
            <c:choose>
              <c:when test="${question.idx == null}">
                    <label for="idx" class="form-label col-auto pe-1 m-0">첨부 파일</label>
                    <div class="col">
                        <label for="fileUpload" id="uploadLabel" class="btn btn-secondary py-0 px-1" style="width: 90px;">파일 선택</label>
                        <input id=fileUpload type="file" accept=".jpg, .jpeg, .png, .bmp, .tiff, .webp, .ico, .svg" style="display: none;">
                        <input type="hidden" id="idx">
                        <input type="hidden" id="count">
                        <div id="thisFile" style="position: relative; display: none;">
                          <input type="hidden" id="thisName" value="${originFileName}">
                          <input type="text" id="fileName" class="form-control" value="${fileName}" readonly>
                          <button id="cancelButton" type="button" class="btn btn-link" style="position: absolute; top: 0; right: 5;">
                            <img alt="XButton" src="${CP}/resources/img/cancel.png" style="height: 25px;">
                          </button>
                        </div>
                     </div>
              </c:when>
              <c:otherwise>
                    <label for="idx" class="form-label col-auto pe-1 m-0">첨부 파일</label>
                    <div class="col">
                        <label for="fileUpload" id="uploadLabel" class="btn btn-secondary py-0 px-1" style="width: 90px; display: none; ">파일 선택</label>
                        <input id=fileUpload type="file" accept=".jpg, .jpeg, .png, .bmp, .tiff, .webp, .ico, .svg" style="display: none;">
                        <input type="hidden" id="idx" value="${question.idx}">
                        <input type="hidden" id="count">
                        <div id="thisFile" style="position: relative;">
                          <input type="hidden" id="thisName" value="${originFileName}">
                          <input type="text" id="fileName" class="form-control" value="${fileName}" readonly>
                          <button id="cancelButton" type="button" class="btn btn-link" style="position: absolute; top: 0; right: 5;">
                            <img alt="XButton" src="${CP}/resources/img/cancel.png" style="height: 25px;">
                          </button>
                        </div>
                    </div>
              </c:otherwise>
            </c:choose>
        </div>
        <div class="mb-3 row">
            <div class="col">
                <textarea class="form-control" id="content" rows="10" placeholder="내용을 입력하세요.">${question.content}</textarea>
            </div>
        </div>

        <div class="text-center">
            <a href="javascript:history.back()" role="button" class="btn btn-secondary">취소</a>
            <button type="button" id="btn-update" class="btn btn-primary"
                value="수정">수정</button>
        </div>

    </form>
    <script src="https://code.jquery.com/jquery-3.7.0.min.js"></script>
    <script
        src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js"></script>
    <script src="/resources/js/qna.js"></script>
</body>
<footer class="py-3 my-4 mt-auto">
  <ul class="nav justify-content-center border-bottom pb-3 mb-3">
  </ul>
  <p class="text-center text-body-secondary">&copy; 2023 F1 RoadScanner Project, All rights reserved.</p>
</footer>
</html>