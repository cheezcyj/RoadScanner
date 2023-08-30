  <%@include file ="/WEB-INF/views/layout/header.jsp" %>

<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%
    // 서버 측에서 사용자 세션을 확인하고, 세션이 없으면 기본 페이지로 리다이렉트합니다.
    if (session.getAttribute("user") == null) {
      response.sendRedirect("/login");
    }
%>
<!-- CSS -->
<link  rel="stylesheet" href="${CP}/resources/css/admin.css" >

<!-- Bootstrap Icons -->
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.3/font/bootstrap-icons.css">
	
  <%@include file ="/WEB-INF/views/layout/navbar.jsp" %>

<body id="font-id"  min-height: 100vh !important;>

<c:if  test="${user ne null}">
<div class ="admin_container">
<br/>

    <!-- <h1 style="margin: auto; text-align:center;">관리자전용 페이지입니다.</h1> -->   
    <iframe id ="member_iframe" src="${CP}/login/list_member"
    style="margin: 50px auto; height: 500px;"></iframe>


<br/>

    <h1 style="margin: auto; text-align:center;"></h1>   
    <iframe id ="admin_iframe" src="${CP}/login/list_admin"
    style="margin: 50px auto; height: 500px;"></iframe>


<br/>


    <h1 style="margin: auto; text-align:center;"></h1>   
    <iframe id ="banned_iframe" src="${CP}/login/list_banned"
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

  <%-- 세션이 없이 해당 페이지 진입하면 로그인 페이지로 이동 --%>
  <c:if test="${user.grade != 2}">
    <script>     
        window.location.href = "/login";
    </script>   
  </c:if>

</body>
<div style="position:relative ">
  <%@include file ="/WEB-INF/views/layout/footer.jsp" %>
</div>