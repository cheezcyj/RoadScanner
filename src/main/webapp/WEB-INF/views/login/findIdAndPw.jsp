  <%@include file ="/WEB-INF/views/layout/header.jsp" %>

<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- CSS -->
<link  rel="stylesheet" href="${CP}/resources/css/default.css" >
<script type="text/javascript"> 
   history.replaceState({}, null, location.pathname); 
</script> 

  <%@include file ="/WEB-INF/views/layout/navbar.jsp" %>

<body id="font-id" class="d-flex flex-column">
	<div style="margin-top: 30px;">
	
	      <div class = "roadscannercontainer" style="margin: 0px auto;"><!-- id 찾기 -->
	      
	        <h1 style="text-align: center; font-weight: 900; font-family: 'Bruno Ace', cursive;">RoadScanner</h1>
	          <h4 style="text-align: center; margin-top:70px; margin-bottom:50px; font-weight: 800;">아이디 찾기</h4>
	          <form class="formabc" onsubmit="return false;">
		          <input type="email" class = "findinput" id="email" name= "email" placeholder="  이메일"><br/>
		          <input type="hidden" id="set_id">
		          <button type="button" class= "btn btn-secondary findbtn" id="findId" name="findId">아이디 찾기</button>
	          </form>
	            <input type ="hidden" id ="id" name ="id">
	            
	      </div><!-- id 찾기 -->
	      
	      <div class = "jb-division-line"></div>
	      
			  <div class = "roadscannercontainer"><!-- pw 찾기 -->
			  
				  <h4 style="text-align: center; margin-bottom:50px; font-weight: 800;">비밀번호 재설정</h4>
			    <form class = "formabc" onsubmit="return false;">
			      <input type="text" class="findinput" style="margin-bottom: 20px;"
			          id="userId" name="userId" onkeyup="id_form_check(event)" placeholder="  아이디"><br/>
			      <input type="email" class="findinput" id="email2" name= "email2" placeholder="  이메일"><br/>
			      <button type="button" class= "btn btn-secondary findbtn" id="findPw" name="findPw">비밀번호 재설정</button>
			    </form>
			    
	      <div style="margin:30px auto;"></div>
			    
		    </div><!-- pw 찾기 -->   
		    
	</div> <!-- container --> 

  <%@include file ="/WEB-INF/views/layout/footer.jsp" %>
  <script src="/resources/js/login/findIdAndPw.js"></script>