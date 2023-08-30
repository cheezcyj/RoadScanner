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
<!-- CSS -->
<link href="${CP}/resources/css/preUpload.css" rel="stylesheet">

  <%@include file ="/WEB-INF/views/layout/navbar.jsp" %>
  
<body id="font-id" class="d-flex flex-column">
  <div class="left">
    <form action="fileUploaded" method="post" enctype="multipart/form-data" onsubmit="return false;">
      <div id="previewGroup" style="display: flex; flex-direction: row;">
        <label id="fileUploadLabel" for="fileUpload" style="display: block; cursor: pointer;">
          <img id="selectButtonImg" alt="SelectButton" src="${CP}/resources/img/selectButton.jpg" width="400" height="400">
        </label>
        <!-- 파일 선택 -->
        <input type="hidden" id="userid" value="${user.id}">
        <input id=fileUpload name="fileUpload" type="file" accept=".jpg, .jpeg, .png, .bmp, .tiff, .webp, .ico, .svg" onchange="displaySelectedFile(event)" style="display: none;">
        <div id="cancelContainer">
          <img id="selectedImage" alt="Selected Image">
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

  <%@include file ="/WEB-INF/views/layout/footer.jsp" %>

  <script src="/resources/js/upload/preUpload.js"></script>
