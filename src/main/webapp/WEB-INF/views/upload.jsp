<header>
  <%@include file ="/WEB-INF/views/layout/header.jsp" %>
</header>  
<%@ page import="org.springframework.ui.Model"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib  prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
    // 서버 측에서 사용자 세션을 확인하고, 세션이 없으면 기본 페이지로 리다이렉트합니다.
    if (session.getAttribute("user") == null) {
      response.sendRedirect("/login");
    }
%>
  <%@include file ="/WEB-INF/views/layout/navbar.jsp" %>
<!-- CSS -->
<link  href="${CP}/resources/css/upload.css" rel="stylesheet">

<body id="font-id" style="min-height: 90vh !important;">
<div id="separation">
<input id="thisName" type="hidden" value="${thisName}">
  <div class="left">
    <form action="fileUploaded" method="post" enctype="multipart/form-data" onsubmit="return false;">
      <div id="previewGroup" style="display: flex; flex-direction: row;">
        <label id="fileUploadLabel" for="fileUpload" style="display: none; cursor: pointer;">
          <img id="selectButtonImg" alt="SelectButton" src="${CP}/resources/img/selectButton.jpg" width="400" height="400">
        </label>
        <!-- 파일 선택 -->
        <input type="hidden" id="userid" value="${user.id}">
        <input id=fileUpload name="fileUpload" type="file" accept=".jpg, .jpeg, .png, .bmp, .tiff, .webp, .ico, .svg" onchange="displaySelectedFile(event)" style="display: none;">
        <div id="cancelContainer" style ="display: block;">
	        <img id="selectedImage" src="${thisUrl}" alt="Selected Image">
	        <button id="cancelButton" type="button" class="btn btn-link">
	          <img alt="XButton" src="${CP}/resources/img/cancel.png">
	        </button>
        </div>
      </div>
      <!-- 모델로 사진 전송, 실행 -->
      <div id="RunContainer" style="display: none;">
        <input class="btn btn-outline-secondary" type="submit" value="표지판 알아보기" id="runButton">
      </div>
    </form>
  </div>

  <div class="divider"></div>

  <div class="right" id="rightContent">
    <!-- 우측 영역의 내용을 입력 -->
    <h3 class="resultImgContent">${resultImg.content}</h3>
    <!-- 세로로 긴 내용 -->
    <div class="resultImgWrapper">
      <img id="resultImg" src="${resultImg.url}" alt="resultImg">
    </div>
    <div>${resultImg.name}</div>
    <p class="notice">의견을 전달해주시면, 이를 활용하여 보다 정확한 서비스를 제공하겠습니다.</p>
    <!-- 피드백 버튼 -->
    <div id="FeedbackButtons">
      <button id="likeButton" type="button" class="btn btn-link"><img src="${CP}/resources/img/thumbsup.jpg" alt="붐업 이미지"></button>
      <button id="dislikeButton" type="button" class="btn btn-link"><img src="${CP}/resources/img/thumbsdown.jpg" alt="붐따 이미지"></button>
    </div>
    <form id="reasonForm" method="post" style="display: none;">
      <div id="dislikeReason" class="card" style="border: none;">
        <div class="card-body">
          <c:forEach var="reason" items="${reasons}" varStatus="loop">
            <div class="form-check">
              <input class="form-check-input" type="checkbox" value="" id="reason${loop.count}" name="reason">
              <label class="form-check-label card-text ms-2" for="reason${loop.count}">
                ${reason}
              </label>
            </div>
          </c:forEach>
          <button class="btn btn-sm btn-secondary" id="submitButton" type="button" style="margin-top: 15px; margin-left: 70px;">선택</button>
        </div>
      </div>
    </form>
  </div>
</div> 

</body>
<div style="position: relative; margin-top: 57px;">
  <%@include file ="/WEB-INF/views/layout/footer.jsp"%>
</div>

<script src="/resources/js/upload/upload.js"></script>
