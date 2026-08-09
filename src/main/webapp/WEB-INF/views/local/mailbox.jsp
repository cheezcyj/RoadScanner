<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@include file="/WEB-INF/views/layout/header.jsp" %>
<body class="rs-internal-page local-mailbox-page d-flex flex-column min-vh-100">
<%@include file="/WEB-INF/views/layout/navbar.jsp" %>

<main class="container py-4 flex-grow-1">
    <div class="d-flex justify-content-between align-items-center mb-3">
        <div>
            <h1 class="h3 mb-1">로컬 메일함</h1>
            <p class="text-muted mb-0">외부로 전송하지 않은 개발용 메시지입니다. 서버를 종료하면 모두 사라집니다.</p>
        </div>
        <form method="post" action="${CP}/local/mailbox/clear">
            <input type="hidden" name="${csrfParameterName}" value="${csrfToken}">
            <button type="submit" class="btn btn-outline-secondary">모두 지우기</button>
        </form>
    </div>

    <c:choose>
        <c:when test="${empty messages}">
            <div class="alert alert-light border">아직 생성된 메시지가 없습니다.</div>
        </c:when>
        <c:otherwise>
            <div class="list-group">
                <c:forEach var="message" items="${messages}">
                    <article class="list-group-item">
                        <div class="d-flex justify-content-between">
                            <strong><c:out value="${message.type}" /></strong>
                            <small class="text-muted"><c:out value="${message.createdAt}" /></small>
                        </div>
                        <div class="text-muted"><c:out value="${message.maskedRecipient}" /></div>
                        <div class="mt-2"><c:out value="${message.contents}" /></div>
                    </article>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</main>

<%@include file="/WEB-INF/views/layout/footer.jsp" %>
