  <%@include file ="/WEB-INF/views/layout/header.jsp" %>
  
<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%
 String strReferer = request.getHeader("referer");
 if(strReferer == null){
%>
 <script language="javascript">
  alert("접속을 차단합니다.");
  document.location.href="/mypage";
 </script>
<%
 return;
 }
%>    
<!-- CSS -->
<link href="${CP}/resources/css/withdraw.css" rel="stylesheet"> <!--.css 파일 연결 -->

  <%@include file ="/WEB-INF/views/layout/navbar.jsp" %>

<body id="font-id" class="d-flex flex-column min-vh-100">
<c:if test="${user ne null }">
    <div class="container" id="for-margin-center">

        <h1 style="text-align: center; margin-bottom: 100px;">RoadScanner</h1>
        <form onsubmit="return false;">
          <label for="password"></label>
          <input type="password" id="rawPassword" name="rawPassword" placeholder="비밀번호를 입력하세요">
        
        <input type="hidden" id="id" name="id" value="${user.id}">
        <div class="wbut">
	       <input type="button" class="btn btn-outline-dark" id="withdraw" value="회원 탈퇴하기">
	    </div>
	    </form>
        </div>
       
</c:if>  
<c:if test="${user eq null}">  <!-- 유저 정보X -->
	<div style="text-align: center; margin:80px; auto;">
	    <h4>로그인 이후 진행해주세요.</h4><p/>
	    <img alt="ddd" src="../resources/img/infinite.gif" loop = 1 >
	    <h4><a href="/login">Go To 로그인</a></h4>
	</div>            
</c:if> <!-- 유저 정보X-end -->

  <%@include file ="/WEB-INF/views/layout/footer.jsp" %>
  <script src="/resources/js/login/withdraw.js"></script>