<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@include file ="/WEB-INF/views/layout/header.jsp" %>
<body>
<%@include file ="/WEB-INF/views/layout/navbar.jsp" %>
    <form class="container mt-4" id="question-form">
        <h1 class="mb-4"><a href="/qna" class="qna-title-link">Q&A 게시판</a></h1>
        <div class="row align-items-center">
	        <c:choose>
	            <c:when test="${user.grade == 2}">  <!-- 관리자 등급인 경우 -->
	                <p id="categoryLabel" class="categoryLabel">[공지]</p>
	                <input type="hidden" id="category" name="category" value="10">
	            </c:when>
	
	            <c:otherwise>  <!-- 일반 사용자인 경우 -->
	                <p id="categoryLabel" class="categoryLabel">[답변대기]</p>
	                <input type="hidden" id="category" name="category" value="30">
	            </c:otherwise>
	        </c:choose>
        </div>

        <div class="row" style="display: none;">
            <label for="id" class="col-sm-2 col-form-label">작성자</label>
            <div class="col-sm-10">자
                <input type="text" class="form-control" id="id" value="${user.id}" readonly>
            </div>
        </div>

        <div class="mb-3 row">
            <div class="col">
                <input type="text" class="form-control" id="title" value="${question.title}" placeholder="제목을 입력하세요.">
                <div id="title-error"></div>
            </div>
        </div>

        <div class="d-flex mb-3 row align-items-center">
            <div class="col">
                <input type="file" id="attachFile" name="attachFile" class="form-control" accept="image/*">
                <div id="attachFile-error"></div>
            </div>
            <!-- 취소 버튼 -->
            <button type="button" id="btn-cancel" class="btn-close"></button>
        </div>

        <div class="mb-3 row">
            <div class="col">
                <textarea class="form-control" id="content" rows="10" placeholder="내용을 입력하세요."></textarea>
                <div id="content-error"></div>
            </div>
        </div>
        <div class="text-center">
            <a href="/qna" role="button" class="btn btn-secondary">취소</a>
            <button type="submit" id="btn-save" class="btn" value="저장">등록</button>
        </div>
    </form>
    <%@include file ="/WEB-INF/views/layout/footer.jsp" %>
