<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib  prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageStylesheet" value="/resources/css/upload.css?v=7" />
<%@include file ="/WEB-INF/views/layout/header.jsp" %>
<body class="rs-internal-page d-flex flex-column min-vh-100">

  <%@include file ="/WEB-INF/views/layout/navbar.jsp" %>
<main id="separation" aria-label="이미지 분석 결과">
<input id="contextPath" type="hidden" value="<c:out value='${CP}'/>">
<input id="thisIdx" type="hidden" value="<c:out value='${upload.idx}'/>">
<input id="feedbackSubmitted" type="hidden" value="${upload.category == 20 or upload.category == 30}">
  <div class="left">
    <div id="cancelContainer">
      <img id="selectedImage" src="<c:out value='${thisUrl}'/>" alt="분석한 이미지 미리보기">
      <a id="cancelButton" class="btn btn-link" href="${CP}/main/preUpload"
          aria-label="사진 분석 초기 화면으로 돌아가기" title="새 이미지 분석">
        <img alt="" aria-hidden="true" src="${CP}/resources/img/cancel.png">
      </a>
    </div>
  </div>

  <div class="right" id="rightContent">
    <div id="analysisResultPanel" class="analysis-result-panel">
      <!-- 우측 영역의 내용을 입력 -->
      <h3 class="resultImgContent"><c:out value="${resultImg.content}"/></h3>
      <!-- 세로로 긴 내용 -->
      <c:if test="${not empty resultImg.url and resultImg.url ne 'none'}">
        <div class="resultImgWrapper">
          <img id="resultImg" src="<c:out value='${resultImg.url}'/>" alt="분류된 표지판 참고 이미지">
        </div>
      </c:if>
      <div><c:out value="${resultImg.name}"/></div>
      <p class="notice">의견을 전달해주시면, 이를 활용하여 보다 정확한 서비스를 제공하겠습니다.</p>
      <!-- 피드백 버튼 -->
      <div id="FeedbackButtons">
        <button id="likeButton" type="button" class="btn btn-link"><img src="${CP}/resources/img/thumbsup.jpg" alt="붐업 이미지"></button>
        <button id="dislikeButton" type="button" class="btn btn-link" aria-controls="reasonForm" aria-expanded="false"><img src="${CP}/resources/img/thumbsdown.jpg" alt="붐따 이미지"></button>
      </div>
      <form id="reasonForm" method="post" style="display: none;">
        <div id="dislikeReason" class="card" style="border: none;">
          <div class="card-body">
            <c:forEach var="reason" items="${reasons}" varStatus="loop">
              <div class="form-check">
                <input class="form-check-input" type="checkbox" value="" id="reason${loop.count}" name="reason">
                <label class="form-check-label card-text ms-2" for="reason${loop.count}">
                  <c:out value="${reason}"/>
                </label>
              </div>
            </c:forEach>
            <button class="btn btn-sm btn-secondary" id="submitButton" type="button">선택</button>
          </div>
        </div>
      </form>
    </div>
  </div>
</main>

<script src="<c:out value='${CP}'/>/resources/js/upload/upload.js?v=9"></script>
<%@include file ="/WEB-INF/views/layout/footer.jsp"%>
