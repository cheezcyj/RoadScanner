<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib  prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="CP" value="${pageContext.request.contextPath}"/> 
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<!-- CSS -->
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-9ndCyUaIbzAi2FUVXJi0CjmCapSmO7SnpJef0486qhLnuZ2cdeRhO02iuK6FUUVM" crossorigin="anonymous">
<link href="https://fonts.googleapis.com/css2?family=Bruno+Ace&display=swap" rel="stylesheet">
<!-- JS -->
<script src="${CP}/resources/js/jquery-3.7.0.js"></script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js" integrity="sha384-geWF76RCwLtnZ8qwWowPQNguL3RmwHVBC9FhGdlKrxdiJJigb/j/68SIy3Te4Bkz" crossorigin="anonymous"></script>
<style> .nav-item {font-size: 15px;} </style>
</head>

<nav id="font-id" class="navbar navbar-expand-md mb-4" style="background-color: white;">
  <div class="container-fluid">
    <a class="navbar-brand" href="${CP}/main" style="font-family: 'Bruno Ace', cursive;">RoadScanner</a>
    <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarCollapse" aria-controls="navbarCollapse" aria-expanded="false" aria-label="Toggle navigation">
      <span class="navbar-toggler-icon"></span>
    </button>
    <div class="collapse navbar-collapse" id="navbarCollapse">
      <ul class="navbar-nav me-auto mb-md-0">
        <c:if test="${user ne null}">
          <li class="nav-item">
            <a class="nav-link" href="${CP}/main/preUpload">사진 업로드</a>
          </li>
          <li class="nav-item">
            <a class="nav-link" href="#">게시판</a>
          </li>
        </c:if>
        <c:if test="${user.grade == 2}">
        <li class="nav-item dropdown">
          <input type="hidden" id="nekeyword" name="nekeyword" value ="${user.id}">
          <a class="nav-link dropdown-toggle" href="#" data-bs-toggle="dropdown" aria-expanded="false">관리자 기능</a>
          <ul class="dropdown-menu">
            <li><a class="dropdown-item" href="${CP}/admin">List</a></li>
            <li><a class="dropdown-item" href="${CP}/imgManagement">Image Management</a></li>
            <li><a class="dropdown-item" href="${CP}/graph">graph</a></li>
            <li><a class="dropdown-item" href="#">None</a></li>
          </ul>
        </li>
        </c:if>
      </ul>
      <form class="d-flex" role="search">
        <!-- 로그인 세션 X -->
        <c:if test="${user eq null}">
          <button type="button" id="login" onclick="location.href='${CP}/login'" class="btn btn-sm btn-outline-secondary me-3">Login</button>
          <button type="button" onclick="location.href='${CP}/registerpage'" class="btn btn-sm btn-outline-secondary" style="margin-right: 50px;">Sign-up</button>
        </c:if>
        <!-- 로그인 세션 O -->
        <c:if test="${user ne null}">
          <button type="button" class="btn btn-sm btn-outline-secondary me-3" onclick="location.href='${CP}/mypage'">MyPage</button>
          <button type="button" class="btn btn-sm btn-outline-secondary" onclick="location.href='${CP}/logout'" style="margin-right: 50px;">LogOut</button>
        </c:if>        
      </form>
    </div>
  </div>
</nav>

</html>