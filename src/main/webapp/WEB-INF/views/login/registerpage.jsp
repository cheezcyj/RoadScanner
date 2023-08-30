  <%@include file ="/WEB-INF/views/layout/header.jsp" %>

<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- CSS -->
<link  href="${CP}/resources/css/membership-style.css" rel="stylesheet">

  <%@include file ="/WEB-INF/views/layout/navbar.jsp" %>

<body class="d-flex flex-column">
  <div class ="reg_container" id="font-id">
    <h1 class="logo">RoadScanner</h1>
    
      <form action="" method="post" name="membership" style="margin: auto;">         
        <ul class= "ccu">
          <li class="cc">
            <label for= "id_form" style="float: left;">아이디</label><br/>
	            <div style="display: flex">
	              <input type="text" name="id_form" id="id_form" onkeyup="id_form_check(event)" onchange="id_length_check()" placeholder="아이디 입력(영어, 숫자포함 6~20자)">
	              <input type="button" class = "btn btn-outline-dark" id="idDulpCheck" value="중복확인" style="margin-left: 15px;">
	            </div>
          </li>
          
					<li class="cc">
					  <label style="float: left;">비밀번호</label><br/>
					  <input type="password" name="pw_form" id="pw_form" placeholder="(문자, 숫자, 특수문자[!,@,#,$,%,*]) 포함 8~20자)" onchange="check_pw()" style="float: left;"><br/>
					</li>
					
					<li class="cc">
					  <label style="float: left;">비밀번호 확인</label><label id="pw_check"></label><br/>
					  <input type="password" name="pw2_form" id="pw2_form" placeholder="비밀번호 재입력" onchange="check_pw()" style="float: left;"><br/>
					</li>
					
					<li class="cc">
            <div>
              <label style="float: left;">이메일 주소</label><br/>
              <div style="display: flex">
                <input type="email" name="email_front" id="email_front" onkeyup="check_email(event)" placeholder="이메일 주소">
                <label style="width:30px; margin-top:7px;">@</label>
                <input type="text" class="listinput" list="email_list" id="email_back" value="">
                <datalist id= "email_list">       
						      <option value="dreamwiz.com">dreamwiz.com</option>
						      <option value="empas.com">empas.com</option>
						      <option value="freechal.com">freechal.com</option>
						      <option value="gmail.com">gmail.com</option>
						      <option value="hanmail.net">hanmail.net</option>
						      <option value="hanmir.com">hanmir.com</option>
						      <option value="hotmail.com">hotmail.com</option>
						      <option value="kakao.com">kakao.com</option>
						      <option value="korea.com">korea.com</option>
						      <option value="lycos.co.kr">lycos.co.kr</option>
						      <option value="nate.com">nate.com</option>
						      <option value="naver.com">naver.com</option>
						      <option value="paran.com">paran.com</option>
						      <option value="yahoo.com">yahoo.com</option>              
                </datalist>
                <input type="button" class="btn btn-outline-dark" id="emailDulpCheck" value="중복확인" style="margin-left: 15px;">
              </div>
            </div>
					</li>

          <li class="cc">
            <label for= "mail-Check-Btn" style="float: left;">E-mail 인증번호 입력</label><br/>
              <div style="display: flex">
		             <input type="button" class = "btn btn-outline-dark nest1" id="mail-Check-Btn" value="인증번호 전송 " class="nest1" >
		             <input type="text" class="emailcheck" name="checkInput" id="checkInput" placeholder="인증번호 6자리를 입력해주세요" maxlength="6" required style="margin-left: 15px; width: 230px;"><br/>
	               <span id="mail-check-warn"></span>
		          </div> 
          </li>        
        </ul>
	      
        <div style="margin-top: 40px; margin-left: -50px;">
          <input type="button" class="btn btn-outline-dark nest1" id="register" value="회원가입">
          <input type="button" class="btn btn-outline-dark nest2" id="noneRegister" value="취소" onclick="firstForm()">
        </div>
      </form>
    
      <form method="POST" name="register_form">
		    <input type="hidden" name="grade" id="grade" value="1">
		    <input type="hidden" name="id" id="id">
		    <input type="hidden" name="pw" id="pw">
		    <input type="hidden" name="email" id="email">  
		    <input type="hidden" name="auth" id="auth" value="1">      
      </form>
				<input type="hidden" name="emailok" id="emailok"> 
				<input type="hidden" name="idok" id="idok"> 
  </div>    

  <%@include file ="/WEB-INF/views/layout/footer.jsp" %>
  <script src="/resources/js/login/register.js"></script>