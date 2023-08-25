  <%@include file ="head.jsp" %>
  
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib  prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
    // 서버 측에서 사용자 세션을 확인하고, 세션이 없으면 기본 페이지로 리다이렉트합니다.
    if (session.getAttribute("user") == null) {
      response.sendRedirect("/login");
    }
%>
<!DOCTYPE html>
<html>
<!-- CSS -->
<link rel="stylesheet"  href="${CP}/resources/css/mypage.css">
<title>로드스캐너 마이페이지</title>

  <%@include file ="navbar.jsp" %>

<body id="font-id" class="d-flex flex-column min-vh-100">
  <!-- 일반 -->
  <c:if  test="${user ne null}">
	  <c:if test="${user.grade == 1}">
	     <h2 style="text-align: center; margin-top: 50px;"><span style="color:#666666;">${user.id}</span>님 의 마이페이지</h2>
	  </c:if>
	  <!-- 관리자 -->
	  <c:if test="${user.grade == 2}">
	     <h2 style="text-align: center; margin-top: 50px;">관리자 <span style="color:#ffc107;">${user.id}</span>님 의 마이페이지</h2>
	  </c:if>
		  <div id="container">
				  <form>
				    <fieldset style="border:0 solid black;">
				      <ul class="list-group" id="for-margin-id" style="list-style: none;">
					      <li>
						      <label>아이디</label><br/>
						      <input class="form-control" type="text" id="rid" readonly="readonly" value="${user.id}">
						    </li>
						    <li>
						      <label>비밀번호 수정</label><br/>
						      <input class="form-control" type="password" id="rpassword" placeholder="문자, 숫자, 특수문자 포함 (8~20글자)" onchange="check_pw()">
						    </li>
						    <li>
						      <label>비밀번호 확인</label>
						      <label id="pw_check" style="margin-left: 10px;"></label><br/>
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
			    <input type="button" class="btn btn-warning" style="color:white;" id="update" value="수정">
			    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
			    <input type="button" class="btn btn-secondary" id="cancel"  value="취소">    
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

</body>

  <%@include file ="footer.jsp" %>
	<style>
	  /* 부트스트랩 호버 수정 */
	  .form-control:focus {
	    box-shadow: none;
	    border-color: #666666;
	  }
	</style>
<script>

function check_pw() {
    var pw = document.getElementById('rpassword').value;
    var num = pw.search(/[0-9]/g);
    var eng = pw.search(/[a-z]/ig);
    var spe = pw.search(/[`~!@@#$%^&*|₩₩₩'₩";:₩/?]/gi);
    
    if(pw.length<8 || pw.length>20) {
       window.alert('비밀번호는 8글자 이상, 20글자 이하만 이용 가능합니다.');
       document.getElementById('rpassword').value='';
       document.getElementById('rpassword').focus();
    } else if(pw.search(/\s/) != -1) {
       window.alert('비밀번호는 공백 없이 이용 가능합니다');
       document.getElementById('rpassword').value='';
       document.getElementById('rpassword').focus();
    } else if(num < 0 || eng < 0 || spe < 0) {
       window.alert('영문, 숫자, 특수문자를 최소 1글자 이상씩 사용하여 입력해주세요');
       document.getElementById('rpassword').value='';
       document.getElementById('rpassword').focus();
    }           
    
    if(document.getElementById('rpassword').value !='' && document.getElementById('rpassword2').value!='') {
      
        if(document.getElementById('rpassword').value == document.getElementById('rpassword2').value) {
            document.getElementById('pw_check').innerHTML='비밀번호가 일치합니다.'
            document.getElementById('pw_check').style.color='blue';
            document.getElementById('pw_check').style.fontSize='15px';
        } else {
            document.getElementById('pw_check').innerHTML='비밀번호가 일치하지 않습니다.';
            document.getElementById('pw_check').style.color='red';
            document.getElementById('pw_check').style.fontSize='15px';
            document.getElementById('rpassword2').value='';
            document.getElementById('rpassword2').focus();
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

    	window.location.href="${CP}/main/preUpload";
    
  });   // $("#cancel") click
   
  $("#update").on("click", function(){
    if(""==$("#rpassword").val() || 0==$("#rpassword").val().length){
          alert("비밀번호를 입력하세요");  // javascript 메시지 다이얼 로그
          $("#rpassword").focus();
          return;
          
    } else {

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
		          success:function(data){
		              let parsedJSON = JSON.parse(data);
		              
		              // 업데이트 성공
		              if("10" == parsedJSON.msgId) {
		                alert(parsedJSON.msgContents);
		                window.location.href="${CP}/logout";
		              }
		              
		              // 업데이트  실패 (입력값 오류)
		              if("20" == parsedJSON.msgId) {
		                    alert(parsedJSON.msgContents);
		                    return;
		                }
		              
		              // 업데이트 실패 (현재 비밀번호와 동일한 값 입력)
		              if("30" == parsedJSON.msgId) {
		                    alert(parsedJSON.msgContents);
		                    return;
		                }           
		              
		            },
		            error:function(data){//실패시 처리
		              console.log("error:"+data);
		            }
		            
		        });  // ajax end
        
    }   // else end
    
  });   // $("#update") click
  
});   //모든 화면이 다 로딩이 되면 실행하는 영역

</script>
</html>