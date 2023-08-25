<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="CP" value="${pageContext.request.contextPath}"/>
<%
 String strReferer = request.getHeader("referer");
 if(strReferer == null){
%>
 <script language="javascript">
  alert("접속을 차단합니다.");
  document.location.href="${CP}/mypage";
 </script>
<%
 return;
 }
%>    
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8"> 
    <!-- CSS -->
    <link href="${CP}/resources/css/withdraw.css" rel="stylesheet"> <!--.css 파일 연결 -->
    <link href="${CP}/resources/css/bootstrap/bootstrap.min.css" rel="stylesheet" crossorigin="anonymous">
    <script src="${CP}/resources/js/bootstrap/bootstrap.bundle.min.js" crossorigin="anonymous"></script>
    <script src="${CP}/resources/js/jquery-3.7.0.js"></script>
    <title>로드스캐너 탈퇴</title>
</head>
<nav class="navbar navbar-expand-md mb-4" style="background-color: white;">
  <div class="container-fluid">
    <a class="navbar-brand" href="#">RoadScanner</a>
    <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarCollapse" aria-controls="navbarCollapse" aria-expanded="false" aria-label="Toggle navigation">
      <span class="navbar-toggler-icon"></span>
    </button>
    <div class="collapse navbar-collapse" id="navbarCollapse">
      <ul class="navbar-nav me-auto mb-2 mb-md-0">
        <li class="nav-item">
          <a class="nav-link active" aria-current="page" href="#">Home</a>
        </li>
        <li class="nav-item">
          <a class="nav-link" href="#">Link</a>
        </li>
        <li class="nav-item">
          <a class="nav-link disabled" aria-disabled="true">Disabled</a>
        </li>
      </ul>
      <form class="d-flex" role="search">
        <!-- 로그인 세션 X -->
        <%-- <c:if test="${user eq null}">
          <button type="button" id="login" onclick="location.href='${CP}/login'" class="btn btn-outline-primary me-2">Login</button>
        </c:if> --%>
        <!-- 로그인 세션 O -->
        <c:if test="${user ne null}">
          <button type="button" class="btn btn-outline-primary me-2" onclick="location.href='${CP}/mypage'">MyPage</button>
          <button type="button" class="btn btn-outline-primary me-2" onclick="location.href='${CP}/logout'">LogOut</button>
        </c:if>
          <button type="button" onclick="location.href='${CP}/registerpage'" class="btn btn-outline-primary" style="margin-right: 50px;">Sign-up</button>
      </form>
    </div>
  </div>
</nav>
<body class="d-flex flex-column min-vh-100">
<c:if test="${user ne null }">
    <div class="container">
        <h1 style="text-align: center; margin-top: 100px; margin-bottom: 100px;">RoadScanner</h1>
        <form>
          <label for="password"></label>
          <input type="password" id="upassword"  placeholder="비밀번호를 입력하세요">
        </form>
        <input type="hidden" id="uid"  value="${user.id}">
        <input type="hidden" id="upw"  value="${user.password}">
        <div class="wbut">
	       <input type="button" class="btn btn-outline-dark" id="withdraw" value="회원 탈퇴하기">
	    </div>
    
    </div>
</c:if>  
<c:if test="${user eq null}">  <!-- 유저 정보X -->
	<div style="text-align: center; margin:80px; auto;">
	    <h4>로그인 이후 진행해주세요.</h4><p/>
	    <img alt="ddd" src="../resources/img/infinite.gif" loop = 1 >
	    <h4><a href="${CP}/login">Go To 로그인</a></h4>
	</div>            
</c:if> <!-- 유저 정보X-end -->
   
</body>
<footer class="py-3 my-4 mt-auto">
  <ul class="nav justify-content-center border-bottom pb-3 mb-3">
  </ul>
  <p class="text-center text-body-secondary">&copy; 2023 F1 RoadScanner Project, All rights reserved.</p>
</footer>
<script>
   $(document).ready(function() {

       $("#withdraw").click(function() {
           var password = $("#upassword").val();

           // 확인 메시지 표시
           if (!confirm('회원 탈퇴하시겠습니까?') == true) {
               return false;
           }
           
           if($("#upassword").val() != $("#upw").val()) {
        	   alert("비밀번호를 다시 확인해주세요")
        	   document.getElementById('upassword').value='';
        	   $('#upassword').focus();
           }
               
           else {


	           // AJAX 요청을 보냅니다.
	           $.ajax({
	               type: "POST",
	               url:"${CP}/withdraw",
	               dataType:"html",
	               data: {
	                id: $("#uid").val()
	               },
	               success:function(data) {
	                let parsedJSON = JSON.parse(data);
	                  
	                    if("10" == parsedJSON.msgId){
	                          alert(parsedJSON.msgContents);
	                          window.location.href="${CP}/login";
	                   } 
	                                         
	                   if("20" == parsedJSON.msgId){
	                       alert(parsedJSON.msgContents);
	                       return;
	                   }
	                   
	               },
	               error: function(data) {
	                   console.log("error:" + data);
	               }
	           }); // --ajax
           
           } // -- else
        	   
       }); // --doWithdraw
       
   });
</script>
</html>