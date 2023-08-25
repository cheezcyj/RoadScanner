<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<html>
<head>
<title>로드스캐너 - 게시판 상세</title>
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
    <div class="container mt-4">
        <h1 class="mb-4">Q&A 게시판</h1>
        <div class="card mb-5">
            <input type="hidden" id="no" value="${question.no}">
            <div class="card-header">
                <div class="d-flex justify-content-between align-items-center">
                    <h2 class="card-title mb-0 me-auto">${question.title}</h2>
                    <h6 class="card-title mb-0">조회수: ${question.views}</h6>
                </div>
            </div>
            <div class="row m-2 mb-0">
                <div class="col">
                    <p class="card-text"><b>작성자: ${question.id}</b>&emsp;( 작성일: ${question.createDate} )
                    <c:if test="${question.updateDate != null}">&ensp;( 최종 수정일: ${question.updateDate} )</c:if>
                    </p>
                </div>
            </div>
            <div class="row m-2">
                <div class="col">
                    <div class="card">
                        <div class="card-body">
                            <c:if test="${question.idx != null}">
                                <input type="image" id="detailImage" src="${img}" style="width: 500px;">
                            </c:if>
                            <p class="card-text">${question.content}</p>
                        </div>
                    </div>
                </div>
            </div>
            <div class="row mx-2 mb-2">
                <div class="col-auto">
                    <a href="/qna/update/${question.no}" class="btn"
                        style="background-color: #DCDCDC;">수정</a>
                    <button type="button" id="btn-delete" class="btn"
                        style="background-color: #DCDCDC;">삭제</button>
                </div>
                <div class="col-auto ms-auto">
                    <a href="/qna" class="btn btn-primary">목록</a>
                </div>
            </div>
        </div>

        <!-- 답변 내용 -->
        <c:if test="${question.category != 10}">
            <div class="mb-5" id="answer-detail">
                <div class="card">
                    <div class="card-header">
                        <h4 class="card-title mb-0">답변</h4>
                    </div>
                    <c:choose>
                        <c:when test="${answer == null}">
                            <!-- 답변이 없을 경우 답변 등록 폼을 표시 -->
                            <form id="answer-form" class="pb-0 mb-0">
                                <div class="d-flex align-items-center mx-4">
                                    <!-- 나중에 관리자 session값 주고 readonly로 변경 예정 -->
                                    <label for="id" class="form-label me-2 my-2">작성자: ${answer.id}</label>
                                </div>
                                <div class="mb-2 mx-4">
                                    <textarea class="form-control" id="answer-content" rows="5"
                                        placeholder="답변을 입력하세요."></textarea>
                                </div>
                            </form>
                            <div class="mb-2 mx-4">
                                <button type="submit" id="btn-answer-save" class="btn"
                                    style="background-color: #DCDCDC;" value="저장">등록</button>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <!-- 답변이 있는 경우 답변 내용을 표시 -->
                            <div class="row m-2">
                                <div class="col">
                                    <!-- 나중에 관리자 session 가져올 예정 -->
                                    <p class="card-text"><b>작성자: ${answer.id}</b>&emsp;( 작성일: ${answer.createDate} )
                                    <c:if test="${answer.updateDate != null}">&ensp;( 최종 수정일: ${answer.updateDate} )</c:if>
                                    </p>
                                </div>
                            </div>
                            <div class="row mx-2 mb-1">
                                <div class="col">
                                    <div class="card">
                                        <div class="card-body py-2">
                                            <p class="card-text">${answer.content}</p>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div class="row m-2 mt-1">
                                <div class="col-auto">
                                    <button type="button" id="btn-answer-update-form" class="btn"
                                        style="background-color: #DCDCDC;">수정</button>
                                    <button type="button" id="btn-answer-delete" class="btn"
                                        style="background-color: #DCDCDC;">삭제</button>
                                </div>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
            <!-- 답변 수정 버튼을 누를 경우 답변 수정 form 표시 -->
            <form id="answer-update-form" class="pb-0 mb-0"
                style="display: none;">
                <div class="mb-5">
                    <div class="card">
                        <div class="card-header">
                            <h4 class="card-title mb-0">답변</h4>
                        </div>
                        <div class="row mt-2 mx-2">
                            <p for="update-id" class="card-text"><b>작성자: ${answer.id}</b></p> 
                        </div>

                        <div class="mt-2 mb-2 mx-4">
                            <textarea class="form-control" id="answer-update-content"
                                rows="5" placeholder="내용을 입력하세요.">${answer.content}</textarea>
                        </div>

                        <div class="mb-2 mx-4">
                            <button type="button" id="btn-answer-cancel-update" class="btn"
                                style="background-color: #DCDCDC;">취소</button>
                            <button type="submit" id="btn-answer-updated" class="btn"
                                style="background-color: #DCDCDC;" value="수정">완료</button>
                        </div>
                    </div>
                </div>
            </form>
        </c:if>
    </div>

    <!-- 부트스트랩 JS 및 Popper.js 추가 -->
    <script src="https://code.jquery.com/jquery-3.7.0.min.js"></script>
    <script
        src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js"></script>
    <script src="/resources/js/qna.js"></script>
    
    <!-- 버튼을 누르면 페이지 새로고침 -->
    <script>
    const cancelButton = document.getElementById("btn-answer-cancel-update");

    cancelButton.addEventListener("click", () => {
        location.reload(); // 페이지 새로고침
    });
    </script>
</body>
<footer class="py-3 my-4 mt-auto">
  <ul class="nav justify-content-center border-bottom pb-3 mb-3">
  </ul>
  <p class="text-center text-body-secondary">&copy; 2023 F1 RoadScanner Project, All rights reserved.</p>
</footer>
</html>