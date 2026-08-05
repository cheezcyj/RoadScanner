<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageStylesheet" value="/resources/css/preUpload.css?v=9" />
<%@include file ="/WEB-INF/views/layout/header.jsp" %>
<body class="rs-internal-page d-flex flex-column min-vh-100">

  <%@include file ="/WEB-INF/views/layout/navbar.jsp" %>

  <input id="contextPath" type="hidden" value="<c:out value='${CP}'/>">

  <main class="rs-page-shell upload-shell">
    <header class="rs-page-heading">
      <p class="rs-page-eyebrow">IMAGE ANALYSIS</p>
      <h1 class="rs-page-title">교통표지판 사진 분석</h1>
      <span class="rs-page-description">선명한 이미지를 업로드하면 교통표지판을 분석해 드립니다.</span>
    </header>

    <section class="upload-card" aria-labelledby="upload-title">
      <form id="analysisUploadForm" action="${CP}/main/fileUploaded" method="post"
          enctype="multipart/form-data" novalidate>
        <input id="fileUpload" class="upload-file-input" name="fileUpload" type="file"
            accept="image/jpeg,image/png,image/bmp,.jpg,.jpeg,.png,.bmp"
            aria-label="분석할 이미지 파일 선택" aria-describedby="upload-requirements uploadStatus">

        <div id="fileUploadLabel" class="upload-dropzone">
          <span class="upload-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" focusable="false">
              <path d="M12 16V4m0 0L7.5 8.5M12 4l4.5 4.5M5 14v4.25C5 19.22 5.78 20 6.75 20h10.5c.97 0 1.75-.78 1.75-1.75V14"/>
            </svg>
          </span>
          <h2 id="upload-title">파일을 업로드해주세요</h2>
          <p>이미지를 이곳에 끌어다 놓거나 아래 버튼을 눌러 선택하세요.</p>
          <label id="chooseFileButton" class="upload-select-button" for="fileUpload"
              role="button" tabindex="0">파일 선택</label>
          <div id="upload-requirements" class="upload-requirements">
            <span>JPG · JPEG · PNG · BMP</span>
            <span>최대 5MB</span>
          </div>
        </div>

        <section id="selectedFilePanel" class="selected-file-panel" aria-labelledby="selected-file-title" hidden>
          <div class="selected-preview">
            <img id="selectedImage" alt="선택한 이미지 미리보기">
          </div>
          <div class="selected-file-details">
            <p class="selected-file-kicker">SELECTED IMAGE</p>
            <h2 id="selected-file-title">선택한 이미지</h2>
            <div class="selected-file-info">
              <span class="file-type-badge" aria-hidden="true">IMG</span>
              <span class="file-copy">
                <strong id="selectedFileName"></strong>
                <small id="selectedFileMeta"></small>
              </span>
            </div>
            <div class="selected-file-actions">
              <label id="replaceFileButton" class="replace-file-button" for="fileUpload"
                  role="button" tabindex="0">다른 파일 선택</label>
              <button id="cancelButton" class="remove-file-button" type="button">선택 취소</button>
            </div>
          </div>
        </section>

        <p id="uploadStatus" class="upload-status" role="status" aria-live="polite"></p>

        <div id="RunContainer" class="run-container" hidden>
          <button id="runButton" class="analysis-run-button" type="submit">
            <span class="run-button-label">이미지 분석 시작</span>
            <span class="run-button-arrow" aria-hidden="true">→</span>
          </button>
        </div>
      </form>
    </section>
  </main>

  <script src="<c:out value='${CP}'/>/resources/js/upload/preUpload.js?v=4"></script>
  <%@include file ="/WEB-INF/views/layout/footer.jsp" %>
