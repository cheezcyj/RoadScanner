<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageStylesheet" value="/resources/css/qna.css?v=11" />
<%@include file ="/WEB-INF/views/layout/header.jsp" %>
<c:set var="resolvedReturnPath" value="${empty returnPath ? (inquiryMode ? '/qna/my' : '/qna') : returnPath}" />

<body class="rs-internal-page d-flex flex-column min-vh-100">
    <%@include file ="/WEB-INF/views/layout/navbar.jsp" %>
    <form class="rs-page-shell" id="question-edit-form">
        <header class="rs-page-heading">
            <c:choose>
                <c:when test="${inquiryMode}">
                    <p class="rs-page-eyebrow">PRIVATE INQUIRY</p>
                    <h1 class="rs-page-title"><a href="${CP}${resolvedReturnPath}" class="qna-title-link">문의 수정</a></h1>
                    <p class="rs-page-description">문의 내용과 첨부 이미지를 수정할 수 있습니다.</p>
                </c:when>
                <c:otherwise>
                    <p class="rs-page-eyebrow">ROADSCANNER Q&amp;A</p>
                    <h1 class="rs-page-title"><a href="${CP}${resolvedReturnPath}" class="qna-title-link">Q&amp;A 게시글 수정</a></h1>
                    <p class="rs-page-description">게시글 내용과 첨부 이미지를 수정할 수 있습니다.</p>
                </c:otherwise>
            </c:choose>
        </header>
        <!-- 이 부분에 히든 필드 추가 -->
        <input type="hidden" id="no" value="${question.no}">
        <input type="hidden" id="idx" value="${question.idx}">
        <input type="hidden" id="storedFileName" value="<c:out value='${originFileName}'/>">
        <input type="hidden" id="questionMode" value="${inquiryMode ? 'inquiry' : 'board'}">
        <input type="hidden" id="returnPath" value="<c:out value='${resolvedReturnPath}'/>">
        
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
                    value="<c:out value='${question.title}'/>" placeholder="제목을 입력하세요.">
            </div>
        </div>

        <div class="d-flex mb-3 row align-items-center">
            <div class="col">
                <div class="input-group">
                    <input type="file" id="attachFile" name="attachFile" class="form-control" accept=".jpg,.jpeg,.png,.bmp" style="display:none;">
                    <button class="btn btn-outline-secondary" type="button" id="btn-select-file">파일 선택</button>
                    <input type="text" id="fileText" class="form-control" value="<c:out value='${fileName}'/>" placeholder="첨부 파일 없음" readonly>
                    <button id="btn-remove-file" class="btn btn-outline-secondary" type="button">삭제</button>
                    <input type="hidden" id="isFileChanged" name="isFileChanged" value="false">
                </div>
            </div>
        </div>
        <div class="mb-3 row">
            <div class="col">
                <c:set var="editorInitialContent" value="${question.content}" />
                <%@include file="/WEB-INF/views/qna/editor.jsp" %>
            </div>
        </div>

        <div class="qna-form-actions">
            <a href="${CP}${resolvedReturnPath}" role="button" class="btn btn-secondary">취소</a>
            <button type="button" id="btn-update" class="btn btn-primary"
                value="수정">수정</button>
        </div>

    </form>

<script src="${CP}/resources/js/qna-editor.js?v=3"></script>
<script src="${CP}/resources/js/qna.js?v=5"></script>
<%@include file ="/WEB-INF/views/layout/footer.jsp" %>
