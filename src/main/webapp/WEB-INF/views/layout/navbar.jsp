<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<nav id="font-id" class="navbar navbar-expand-md mb-4" style="background-color: #dcdcdc; opacity: 0.9;">
    <div class="container-fluid">
        <a class="navbar-brand roadscanner" href="${CP}/main" style="font-size: 22px;">RoadScanner</a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarCollapse" aria-controls="navbarCollapse" aria-expanded="false" aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarCollapse">
            <ul class="navbar-nav me-auto mb-md-0 mt-1">
                <c:if test="${user ne null}">
                    <li class="nav-item" style="font-size: 18px;">
                        <a class="nav-link" href="${CP}/main/preUpload">사진 업로드</a>
                    </li>
                    <li class="nav-item" style="font-size: 18px;">
                        <a class="nav-link" href="${CP}/qna">게시판</a>
                    </li>
                </c:if>
                <c:if test="${user.grade == 2}">
                    <li class="nav-item dropdown" style="font-size: 18px;">
                        <input type="hidden" id="nekeyword" name="nekeyword" value ="${user.id}">
                        <a class="nav-link dropdown-toggle" href="#" data-bs-toggle="dropdown" aria-expanded="false">관리자 기능</a>
                        <ul class="dropdown-menu">
                            <li><a class="dropdown-item" href="${CP}/admin">List</a></li>
                            <li><a class="dropdown-item" href="${CP}/imgManagement">Image Management</a></li>
                            <li><a class="dropdown-item" href="${CP}/graph">graph</a></li>
                        </ul>
                    </li>
                </c:if>
            </ul>
            <c:if test="${user ne null}">
                <div>
                    <p id="welcome">${user.id}님, 환영합니다!</p>
                </div>
            </c:if>
            <form class="d-flex" role="search" style="margin-top: 9px; margin-bottom: 0px;">
                <c:choose>
                    <c:when test="${user ne null}">
                        <button type="button" class="btn btn-md btn-outline-dark me-2" onclick="location.href='${CP}/mypage'">MyPage</button>
                        <button type="button" class="btn btn-md btn-outline-dark" onclick="location.href='${CP}/logout'" style="margin-right: 50px;">LogOut</button>
                    </c:when>
                    <c:otherwise>
                        <button type="button" class="btn btn-md btn-outline-dark me-2" id="login" onclick="location.href='${CP}/login'" >Login</button>
                        <button type="button" class="btn btn-md btn-outline-dark" onclick="location.href='${CP}/registerpage'" style="margin-right: 50px;">Sign-up</button>
                    </c:otherwise>
                </c:choose>
            </form>
        </div>
    </div>
</nav>
