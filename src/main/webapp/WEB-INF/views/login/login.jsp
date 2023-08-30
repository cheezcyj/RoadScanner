  <%@include file ="/WEB-INF/views/layout/header.jsp" %>

<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- CSS -->
<link  rel="stylesheet" href="${CP}/resources/css/default.css">
<title>로드스캐너 로그인</title>

<body id="font-id" class="d-flex flex-column min-vh-100">
<c:if test="${user eq null}"> 
   <a href="${CP}/main" id="head-logo">RoadScanner</a>
    <div class="roadscannercontainer">
	    <form onsubmit="return false;"> 
	        <div class="loginbox">
	
	            <div class="loginboxdiv">
	              <img class="icon" src="${CP}/resources/img/usericon.png"></img>
	              <input class="loginidpwbtn" type="text" id="id" name="id" placeholder="아이디"><p/>
		          </div>
		         
			        <div class="loginboxdiv" style="margin-top: 20px">
			          <img class="icon"  src="${CP}/resources/img/passwordicon.png"></img>
			          <input class="loginidpwbtn" type="password" id="pw" name= "pw" placeholder="비밀번호"><p/>
			        </div>
			        
		          <button type="submit" class="btn btn-warning loginbtn" id="doLogin" name="doLogin">로그인</button><p/>
	         
	            <div id="button-div">
		            <button type="button" class="btn btn-outline-dark for-btn-center"
		              onclick="window.location.href='${CP}/findIdPw';">ID/PW찾기</button>
			           
			          <button type="button" class="btn btn-outline-dark for-btn-center" 
		              onclick=" window.location.href='${CP}/registerpage';">회원가입</button>
	            </div>
		    </div>  <!-- class = loginbox  -->
		  </form> 
    </div>
</c:if> <!-- 유저 정보X-end -->   

<c:if test="${user ne null}">  <!-- 유저 정보O -->
    <div style="text-align: center; margin:80px; auto;">
        <h3>현재 로그인 상태입니다.</h3><p/>
        <h4>로그아웃 이후 진행해주세요.</h4><p/>
        <img alt="ddd" src="../resources/img/infinite.gif" loop = 1 >
        <h4><a href="${CP}/logout">Go To 로그아웃</a></h4>
    </div>            
</c:if> <!-- 유저 정보O-end -->


  <%@include file ="/WEB-INF/views/layout/footer.jsp" %>
  <script src="/resources/js/login/login.js"></script>