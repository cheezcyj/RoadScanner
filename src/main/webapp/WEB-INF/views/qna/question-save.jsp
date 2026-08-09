<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<c:set var="pageStylesheet" value="/resources/css/qna.css?v=11" />
<%@include file ="/WEB-INF/views/layout/header.jsp" %>
<c:set var="resolvedReturnPath" value="${empty returnPath ? (inquiryMode ? '/qna/my' : '/qna') : returnPath}" />
<body class="rs-internal-page d-flex flex-column min-vh-100">
    <%@include file ="/WEB-INF/views/layout/navbar.jsp" %>
    <form class="rs-page-shell" id="question-form">
        <header class="rs-page-heading">
            <c:choose>
                <c:when test="${inquiryMode}">
                    <p class="rs-page-eyebrow">PRIVATE INQUIRY</p>
                    <h1 class="rs-page-title"><a href="${CP}${resolvedReturnPath}" class="qna-title-link">문의 작성</a></h1>
                    <p class="rs-page-description">작성한 문의는 본인과 관리자만 확인할 수 있습니다.</p>
                </c:when>
                <c:otherwise>
                    <p class="rs-page-eyebrow">ROADSCANNER Q&amp;A</p>
                    <h1 class="rs-page-title"><a href="${CP}${resolvedReturnPath}" class="qna-title-link">Q&amp;A 게시판 글쓰기</a></h1>
                    <p class="rs-page-description">모든 사용자가 확인할 수 있는 게시글입니다.</p>
                </c:otherwise>
            </c:choose>
        </header>
        <input type="hidden" id="idx" value="">
        <input type="hidden" id="questionMode" value="${inquiryMode ? 'inquiry' : 'board'}">
        <input type="hidden" id="returnPath" value="<c:out value='${resolvedReturnPath}'/>">
        <div class="row align-items-center">
	        <c:choose>
	            <c:when test="${inquiryMode}">
	                <p id="categoryLabel" class="categoryLabel">[문의]</p>
	                <input type="hidden" id="category" name="category" value="30">
	            </c:when>
	            <c:when test="${user.grade == 2}">  <!-- 관리자 등급인 경우 -->
	                <p id="categoryLabel" class="categoryLabel">[공지]</p>
	                <input type="hidden" id="category" name="category" value="10">
	            </c:when>
	
	            <c:otherwise>  <!-- 일반 사용자인 경우 -->
	                <p id="categoryLabel" class="categoryLabel">[일반]</p>
	                <input type="hidden" id="category" name="category" value="40">
	            </c:otherwise>
	        </c:choose>
        </div>

        <div class="row" style="display: none;">
            <label for="id" class="col-sm-2 col-form-label">작성자</label>
            <div class="col-sm-10">
                <input type="text" class="form-control" id="id" value="${user.id}" readonly>
            </div>
        </div>
        <div class="mb-3 row">
            <div class="col">
                <input type="text" class="form-control" id="title" placeholder="제목을 입력하세요.">
            </div>
        </div>
        <div class="d-flex mb-3 row align-items-center">
            <label for="idx" class="form-label col-auto pe-1 m-0">첨부 파일</label>
            <div class="col">
                <div class="col">
                    <div class="input-group">
                        <input type="file" id="attachFile" name="attachFile" class="form-control" accept=".jpg,.jpeg,.png,.bmp" style="display:none;">
                        <button class="btn btn-outline-secondary" type="button" id="btn-select-file">파일 선택</button>
                        <input type="text" id="fileText" class="form-control" placeholder="첨부 파일 없음" readonly>
                        <button id="btn-remove-file" class="btn btn-outline-secondary" type="button">삭제</button>
                    </div>
                </div>
            </div>
        </div>
        <div class="mb-3 row">
            <div class="col">
                <c:set var="editorInitialContent" value="" />
                <%@include file="/WEB-INF/views/qna/editor.jsp" %>
            </div>
        </div>

        <div class="qna-form-actions">
            <a href="${CP}${resolvedReturnPath}" role="button" class="btn btn-secondary">취소</a>
            <button type="submit" id="btn-save" class="btn btn-primary"
                value="저장">등록</button>
        </div>
    </form>

<script src="${CP}/resources/js/qna-editor.js?v=3"></script>
<script src="${CP}/resources/js/qna.js?v=5"></script>
<%@include file ="/WEB-INF/views/layout/footer.jsp" %>

