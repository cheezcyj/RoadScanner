<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib  prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
    // 서버 측에서 사용자 세션을 확인하고, 세션이 없으면 기본 페이지로 리다이렉트합니다.
    if (session.getAttribute("user") == null) {
      response.sendRedirect("/login");
    }
%>
<c:set var="CP" value="${pageContext.request.contextPath}"/> 
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="stylesheet"  href="${CP}/resources/css/mypage.css">
<link href="https://fonts.googleapis.com/css2?family=Nanum+Pen+Script&family=Noto+Sans+KR&display=swap" rel="stylesheet">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link  href="${CP}/resources/css/bootstrap/bootstrap.min.css" rel="stylesheet"  crossorigin="anonymous">
<script src="${CP}/resources/js/bootstrap/bootstrap.bundle.min.js"  crossorigin="anonymous"></script>
<script src="${CP}/resources/js/jquery-3.7.0.js"></script>
<title>로드스캐너 마이페이지</title>
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
        <c:if test="${user.grade == 2}">
        <li class="nav-item dropdown">
          <input type="hidden" id="nekeyword" name="nekeyword" value ="${user.id}">
          <a class="nav-link dropdown-toggle" href="#" data-bs-toggle="dropdown" aria-expanded="false">관리자 기능</a>
          <ul class="dropdown-menu">
            <li><a class="dropdown-item" href="${CP}/admin">List</a></li>
            <li><a class="dropdown-item" href="#">None</a></li>
            <li><a class="dropdown-item" href="#">None</a></li>
          </ul>
        </li>
        </c:if>
      </ul>
      <form class="d-flex" role="search">
        <!-- 로그인 세션 X -->
        <c:if test="${user eq null}">
          <button type="button" id="login" onclick="location.href='${CP}/login'" class="btn btn-outline-primary me-2">Login</button>
          <button type="button" onclick="location.href='${CP}/registerpage'" class="btn btn-outline-primary" style="margin-right: 50px;">Sign-up</button>
        </c:if>
        <!-- 로그인 세션 O -->
        <c:if test="${user ne null}">
          <button type="button" class="btn btn-outline-primary" onclick="location.href='${CP}/logout'" style="margin-right: 50px;">LogOut</button>
        </c:if>        
      </form>
    </div>
  </div>
</nav>

  <body class="d-flex flex-column min-vh-100">
  <!-- 일반 -->
  <c:if  test="${user ne null}">
  <c:if test="${user.grade ==1}">
     <h2 style="text-align: center; margin-top: 100px; margin-bottom: 80px;">${user.id}님 의 마이페이지</h2>
  </c:if>
  <!-- 관리자 -->
  <c:if test="${user.grade ==2}">
     <h2 style="text-align: center; margin-top: 100px; margin-bottom: 80px;">관리자 ${user.id}님 의 마이페이지</h2>
  </c:if>
	  <div id="container">
			  <form>
			    <fieldset style="border:0 solid black;">
			      <ul class="list-group" style="list-style: none;">
				      <li>
					      <label>아이디</label><br/>
					      <input class="form-control" type="text" id="rid" readonly="readonly" value="${user.id}">
					    </li>
					    <li>
					      <label>비밀번호 수정</label><br/>
					      <input class="form-control" type="password" id="rpassword" placeholder="문자, 숫자, 특수문자 포함 8~20글자)" >
					    </li>
					    <li>
					      <label>비밀번호 확인</label>
					      <label id="pw_check"></label><br/>
					      <input class="form-control" type="password" id="rpassword2" placeholder="비밀번호 재입력" onchange="check_pw()">
					    </li>
					    <li>
					      <label>이메일</label><br/>
					      <input class="form-control" type="text" id="remail" readonly="readonly" value="${user.email}">
					    </li>
				    </ul>
				    <input type="hidden" id="upw"  value="${user.password}">
			    </fieldset>
			  </form>
			</div>
			<div class="update_btn">
		    <input type="button" class="btn btn-outline-primary" id="update" value="수정">
		    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
		    <input type="button" class="btn btn-outline-danger" id="cancel" value="취소">
	    </div>
	    <c:if test="${user.grade == 1}">
	    <div class="qna_btn">
	       <input type="button" class="btn btn-outline-dark" id="myQnAboard" value="내 QnA보기">
	    </div>
	    <div class="draw_btn">
	       <input type="button" class="btn btn-outline-dark" id="withdraw" value="탈퇴">
	    </div>
	    </c:if>
	</c:if>
	
	<c:if test="${user eq null}">
    <p>세션이 없습니다. 기본 페이지로 리다이렉트합니다.</p>
  </c:if>
	</body>

  <footer class="py-3 my-4 mt-auto">
    <ul class="nav justify-content-center border-bottom pb-3 mb-3">
    </ul>
    <p class="text-center text-body-secondary">&copy; 2023 F1 RoadScanner Project, All rights reserved.</p>
  </footer>

<script>
function check_pw() {
    var pw = document.getElementById('rpassword').value;
    var num = pw.search(/[0-9]/g);
    var eng = pw.search(/[a-z]/ig);
    var spe = pw.search(/[`~!@@#$%^&*|₩₩₩'₩";:₩/?]/gi);
    
    if(document.getElementById('rpassword').value == document.getElementById('upw').value) {
    	window.alert('현재 사용중인 비밀번호 입니다.');
        document.getElementById('rpassword').value='';
        document.getElementById('rpassword2').value='';
    }
    
    if(pw.length<8 || pw.length>20) {
       window.alert('비밀번호는 8글자 이상, 20글자 이하만 이용 가능합니다.');
       document.getElementById('rpassword').value='';
    } else if(pw.search(/\s/) != -1) {
       window.alert('비밀번호는 공백 없이 이용 가능합니다');
       document.getElementById('rpassword').value='';
    } else if(num < 0 || eng < 0 || spe < 0) {
       window.alert('영문, 숫자, 특수문자를 최소 1글자 이상씩 사용하여 입력해주세요');
       document.getElementById('rpassword').value='';
    }           
    
    if(document.getElementById('rpassword').value !='' && document.getElementById('rpassword2').value!='') {
      
        if(document.getElementById('rpassword').value == document.getElementById('rpassword2').value) {
            document.getElementById('pw_check').innerHTML='비밀번호가 일치합니다.'
            document.getElementById('pw_check').style.color='blue';
        } else {
            document.getElementById('pw_check').innerHTML='비밀번호가 일치하지 않습니다.';
            document.getElementById('pw_check').style.color='red';
            document.getElementById('rpassword').value='';
            document.getElementById('rpassword2').value='';
        }
        
    }
    
}   // check_pw end

$(document).ready(function(){  //모든 화면이 다 로딩이 되면 실행하는 영역
   console.log("document ready");
   
   $("#login").on("click", function(){
	   
	   window.location.href="${CP}/login";
	   
   }); // login click
   
   $("#withdraw").on("click", function(){
	      
	    window.location.href="${CP}/withdraw";
	    
	  });   // $("#withdraw") click 

  $("#cancel").on("click", function(){
	  
    alert("로그인페이지로 이동합니다");
    window.location.href="${CP}/login";
    
  });   // $("#cancel") click
  
  $("#update").on("click", function(){
	  
	  $.ajax({
	        type: "POST",
	        url:"${CP}/update",
	        asyn:"true",
	        dataType:"html",
	        data:{
	        	id: $("#rid").val(),
	        	password: $("#rpassword").val(),
	        	email: $("#remail").val()
	        },
	        success:function(data){//통신 성공
	        	  let parsedJSON = JSON.parse(data);
	        	  
	        	  if("10" == parsedJSON.msgId) {
	        		  alert(parsedJSON.msgContents);
	        		  window.location.href="${CP}/login";
	        	  }
	        	  
	        	  if("20" == parsedJSON.msgId) {
                  alert(parsedJSON.msgContents);
                  return;
	             }
	        	  
	          },
	          error:function(data){//실패시 처리
	            console.log("error:"+data);
	          }
	      });  // ajax end
	  
  });   // $("#update") click
  
});   //모든 화면이 다 로딩이 되면 실행하는 영역
</script>
</html>