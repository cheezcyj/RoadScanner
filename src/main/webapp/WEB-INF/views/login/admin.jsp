  <%@include file ="head.jsp" %>

<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%
    // 서버 측에서 사용자 세션을 확인하고, 세션이 없으면 기본 페이지로 리다이렉트합니다.
    if (session.getAttribute("user") == null) {
      response.sendRedirect("/login");
    }
%>
<%
 String strReferer = request.getHeader("referer");
 if(strReferer == null){
%>
 <script language="javascript">
  alert("접속을 차단합니다.");
  document.location.href="${CP}/login";
 </script>
<%
 return;
 }
%>
<!DOCTYPE html>
<html>  

<meta charset="UTF-8">
<!-- CSS -->
<link  rel="stylesheet" href="${CP}/resources/css/admin.css" >

<!-- Bootstrap Icons -->
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.3/font/bootstrap-icons.css">
<title>관리자 페이지</title>
	
  <%@include file ="navbar.jsp" %>

<body id="font-id">

<c:if  test="${user ne null}">
<div class ="admin_container">
<br/>

    <!-- <h1 style="margin: auto; text-align:center;">관리자전용 페이지입니다.</h1> -->   
    <iframe id ="member_iframe" src="http://localhost:8080/login/list_member"
    style="margin: 50px auto; height: 500px;"></iframe>


<br/>

    <h1 style="margin: auto; text-align:center;"></h1>   
    <iframe id ="admin_iframe" src="http://localhost:8080/login/list_admin"
    style="margin: 50px auto; height: 500px;"></iframe>


<br/>


    <h1 style="margin: auto; text-align:center;"></h1>   
    <iframe id ="banned_iframe" src="http://localhost:8080/login/list_banned"
    style="margin: 50px auto; height: 500px;"></iframe>
</div>
</c:if>

<c:if test="${user eq null}">  <!-- 유저 정보X -->
    <div style="text-align: center; margin:80px auto;">
        <h4>로그인 이후 진행해주세요.</h4><p/>
          <img alt="ddd" src="../resources/img/infinite.gif" loop = 1 >
        <h4><a href="${CP}/login">Go To 로그인</a></h4>
    </div>            
</c:if> <!-- 유저 정보X-end -->

</body>

  <%@include file ="footer.jsp" %>

</html>
