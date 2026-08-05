<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<header class="rs-site-header">
<nav class="navbar navbar-expand-md rs-navbar" aria-label="주요 메뉴">
    <div class="container-fluid">
        <a class="navbar-brand roadscanner" href="${CP}/main" aria-label="RoadScanner 메인으로 이동">
            <img class="rs-brand-icon"
                 src="${CP}/resources/img/roadscanner-mark.svg"
                 width="30"
                 height="30"
                 alt=""
                 aria-hidden="true">
            <span>RoadScanner</span>
        </a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarCollapse" aria-controls="navbarCollapse" aria-expanded="false" aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarCollapse">
            <ul class="navbar-nav me-auto">
                <c:if test="${user ne null}">
                    <li class="nav-item">
                        <a class="nav-link" href="${CP}/main/preUpload">사진 분석</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="${CP}/qna">Q&amp;A게시판</a>
                    </li>
                </c:if>
                <c:if test="${user.grade == 2}">
                    <li class="nav-item dropdown">
                        <input type="hidden" id="nekeyword" name="nekeyword" value ="${user.id}">
                        <a class="nav-link dropdown-toggle" href="#" data-bs-toggle="dropdown" aria-expanded="false">관리자 기능</a>
                        <ul class="dropdown-menu">
                            <li><a class="dropdown-item" href="${CP}/admin">Account List</a></li>
                            <li><a class="dropdown-item" href="${CP}/imgManagement">Image Management</a></li>
                            <li><a class="dropdown-item" href="${CP}/graph">graph</a></li>
                            <li><a class="dropdown-item" href="${CP}/qna/inquiries">문의 관리</a></li>
                            <c:if test="${localMailboxEnabled}">
                                <li><a class="dropdown-item" href="${CP}/local/mailbox">로컬 메일함</a></li>
                            </c:if>
                        </ul>
                    </li>
                </c:if>
            </ul>
            <c:if test="${user ne null}">
                <div class="rs-welcome-wrap">
                    <p id="welcome">${user.id}님, 환영합니다!</p>
                </div>
            </c:if>
            <div class="rs-nav-actions">
                <c:choose>
                    <c:when test="${user ne null}">
                        <a class="btn rs-header-action rs-header-action-secondary" href="${CP}/mypage">MyPage</a>
                        <button type="button" class="btn rs-header-action rs-header-action-primary" onclick="window.roadscannerLogout()">LogOut</button>
                    </c:when>
                    <c:otherwise>
                        <a class="btn rs-header-action rs-header-action-primary" id="login" href="${CP}/login">Login</a>
                        <a class="btn rs-header-action rs-header-action-secondary" href="${CP}/registerpage">Sign-up</a>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</nav>
</header>
