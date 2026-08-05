<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageStylesheet" value="/resources/css/qna.css?v=11" />
<%@include file ="/WEB-INF/views/layout/header.jsp" %>
<c:set var="resolvedReturnPath" value="${empty returnPath ? (inquiryMode ? '/qna/my' : '/qna') : returnPath}" />
<body class="rs-internal-page d-flex flex-column min-vh-100">
    <%@include file ="/WEB-INF/views/layout/navbar.jsp" %>
    <main class="rs-page-shell">
        <header class="rs-page-heading">
            <c:choose>
                <c:when test="${inquiryMode and viewMode eq 'adminInquiry'}">
                    <p class="rs-page-eyebrow">INQUIRY MANAGEMENT</p>
                    <h1 class="rs-page-title"><a href="${CP}${resolvedReturnPath}" class="qna-title-link">문의 상세</a></h1>
                    <p class="rs-page-description">사용자 문의 내용과 답변 상태를 확인합니다.</p>
                </c:when>
                <c:when test="${inquiryMode}">
                    <p class="rs-page-eyebrow">MY INQUIRY</p>
                    <h1 class="rs-page-title"><a href="${CP}${resolvedReturnPath}" class="qna-title-link">내 문의 상세</a></h1>
                    <p class="rs-page-description">이 문의는 본인과 관리자만 확인할 수 있습니다.</p>
                </c:when>
                <c:otherwise>
                    <p class="rs-page-eyebrow">ROADSCANNER Q&amp;A</p>
                    <h1 class="rs-page-title"><a href="${CP}${resolvedReturnPath}" class="qna-title-link">Q&amp;A 게시판</a></h1>
                </c:otherwise>
            </c:choose>
        </header>
        <div class="card qna-detail-card mb-5">
            <input type="hidden" id="no" value="${question.no}">
            <input type="hidden" id="storedFileName" value="<c:out value='${storedFileName}'/>">
            <input type="hidden" id="questionMode" value="${inquiryMode ? 'inquiry' : 'board'}">
            <input type="hidden" id="returnPath" value="<c:out value='${resolvedReturnPath}'/>">
            <div class="card-header">
                <div class="qna-title-row d-flex justify-content-between align-items-center">
                    <h2 class="card-title mb-0 me-auto"><c:out value="${question.title}"/></h2>
                    <h6 class="card-title mb-0">조회수: ${question.views}</h6>
                </div>
            </div>
            <div class="row m-2 mb-0">
                <div class="col">
                    <p class="card-text"><b>작성자: <c:out value="${question.id}"/></b>&emsp;( 작성일: <c:out value="${question.createDate}"/> )
                    <c:if test="${question.updateDate != null}">&ensp;( 최종 수정일: ${question.updateDate} )</c:if>
                    </p>
                </div>
            </div>
            <div class="row m-2">
                <div class="col">
                    <div class="card">
                        <div class="card-body">
                            <c:if test="${question.idx != null}">
                                <input type="image" id="detailImage" class="card-image" src="<c:out value='${img}'/>">
                            </c:if>
                            <%-- QuestionResponseDTO에서 허용 목록으로 정화된 본문만 HTML로 표시한다. --%>
                            <div class="qna-rich-content"><c:out value="${question.content}" escapeXml="false"/></div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="row mx-2 mb-2">
                <c:if test="${not empty sessionScope.user and (sessionScope.user.grade == 2 or sessionScope.user.id eq question.id)}">
                    <div class="col-auto qna-button-group">
                        <a href="${CP}/qna/update/${question.no}" class="btn"
                            style="background-color: #DCDCDC;">수정</a>
                        <button type="button" id="btn-delete" class="btn"
                            style="background-color: #DCDCDC;">삭제</button>
                    </div>
                </c:if>
                <div class="col-auto ms-auto qna-button-group">
                    <a href="${CP}${resolvedReturnPath}" class="btn btn-primary">목록</a>
                </div>
            </div>
        </div>

        <!-- 답변 내용 -->
        <c:if test="${inquiryMode and (question.category == 20 or question.category == 30)}">
            <div class="mb-5" id="answer-detail">
                <div class="card">
                    <div class="card-header"><h4 class="card-title mb-0">답변</h4></div>
                    <c:choose>
                        <c:when test="${empty answer}">
                            <c:choose>
                                <c:when test="${not empty sessionScope.user and sessionScope.user.grade == 2}">
                                    <form id="answer-form" class="pb-0 mb-0">
                                        <div class="d-flex align-items-center mx-4">
                                            <label for="answer-content" class="form-label me-2 my-2">작성자: <c:out value="${sessionScope.user.id}"/></label>
                                        </div>
                                        <div class="mb-2 mx-4">
                                            <textarea class="form-control" id="answer-content" rows="5" placeholder="답변을 입력하세요."></textarea>
                                        </div>
                                    </form>
                                    <div class="mb-3 mx-4 qna-button-group">
                                        <button type="button" id="btn-answer-save" class="btn" style="background-color: #DCDCDC;">등록</button>
                                    </div>
                                </c:when>
                                <c:otherwise>
                                    <p class="card-text m-4">관리자 답변을 기다리고 있습니다.</p>
                                </c:otherwise>
                            </c:choose>
                        </c:when>
                        <c:otherwise>
                            <div class="row m-2">
                                <div class="col">
                                    <p class="card-text"><b>작성자: <c:out value="${answer.id}"/></b>&emsp;( 작성일: <c:out value="${answer.createDate}"/> )
                                    <c:if test="${answer.updateDate != null}">&ensp;( 최종 수정일: <c:out value="${answer.updateDate}"/> )</c:if></p>
                                </div>
                            </div>
                            <div class="row mx-2 mb-3">
                                <div class="col"><div class="card"><div class="card-body py-2">
                                    <p class="card-text"><c:out value="${answer.content}"/></p>
                                </div></div></div>
                            </div>
                            <c:if test="${not empty sessionScope.user and sessionScope.user.grade == 2}">
                                <div class="row m-2 mt-1"><div class="col-auto qna-button-group">
                                    <button type="button" id="btn-answer-update-form" class="btn" style="background-color: #DCDCDC;">수정</button>
                                    <button type="button" id="btn-answer-delete" class="btn" style="background-color: #DCDCDC;">삭제</button>
                                </div></div>
                            </c:if>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>

            <c:if test="${not empty answer and not empty sessionScope.user and sessionScope.user.grade == 2}">
                <form id="answer-update-form" class="pb-0 mb-0" style="display: none;">
                    <div class="mb-5"><div class="card">
                        <div class="card-header"><h4 class="card-title mb-0">답변</h4></div>
                        <div class="row mt-2 mx-2"><p class="card-text"><b>작성자: <c:out value="${answer.id}"/></b></p></div>
                        <div class="mt-2 mb-2 mx-4">
                            <textarea class="form-control" id="answer-update-content" rows="5" placeholder="내용을 입력하세요."><c:out value="${answer.content}"/></textarea>
                        </div>
                        <div class="mb-3 mx-4 qna-button-group">
                            <button type="button" id="btn-answer-cancel-update" class="btn" style="background-color: #DCDCDC;">취소</button>
                            <button type="button" id="btn-answer-updated" class="btn" style="background-color: #DCDCDC;">완료</button>
                        </div>
                    </div></div>
                </form>
            </c:if>
        </c:if>
    </main>
    
    <script src="${CP}/resources/js/qna.js?v=5"></script>
<%@include file="/WEB-INF/views/layout/footer.jsp" %>
