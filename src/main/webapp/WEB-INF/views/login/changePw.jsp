  <%@include file ="/WEB-INF/views/layout/header.jsp" %>
  
<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib  prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!-- CSS -->
<link rel="stylesheet"  href="${CP}/resources/css/changePw.css">

  <%@include file ="/WEB-INF/views/layout/navbar.jsp" %>

<body id="font-id" class="d-flex flex-column">
 <h3 style="text-align: center; margin-top: 100px;">비밀번호 재설정 페이지</h3>
  <div id="container">
      <form>
        <fieldset id="none-border">
          <ul class="list-group" style="list-style: none;">
            <li>
              <label>이메일</label><br/>
              <div style="display: flex;">
	              <input class="form-control" type="text" id="remail" placeholder="이메일 주소" onkeyup="check_email(event)"
	                     style="width: 700px; margin-right: 50px;">
	              <input type="button" class="btn btn-warning" id="emailDulpCheck" value="유저 확인" style="color:white;">
              </div>
            </li>
            <li>
             <label for="mail-Check-Btn">이메일 인증번호 입력</label>
             <label id="mail-check-warn" style="margin-left: 10px;"></label><br/>
             <input class="form-control" type="text" class="emailcheck" name="checkInput" id="checkInput" placeholder="인증번호 6자리를 입력해주세요" 
                    maxlength="6" required="required" style="width: 655px;">
            </li> 
            <li>
              <label>비밀번호 수정</label><br/>
              <input class="form-control" type="password" id="rpassword" placeholder="문자, 숫자, 특수문자 포함 (8~20글자)" onchange="check_pw()">
            </li>
            <li>
              <label>비밀번호 확인</label>
              <label id="pw_check" style="margin-left: 10px; text-align: center;"></label><br/>
              <input class="form-control" type="password" id="rpassword2" placeholder="비밀번호 재입력" onchange="check_pw()">
            </li>
          </ul>
        </fieldset>
      </form>
      <form method="POST" name="register_form">
            <input type="hidden" name="pw" id="pw">
            <input type="hidden" name="email" id="email">         
      </form>
        <input type="hidden" name="emailok" id="emailok"> 
    </div>
    <div class="update_btn">
      <input type="button" class="btn btn-warning" id="changePw" value="변경" style="color:white;">
      <input type="button" class="btn btn-secondary" id="cancle" value="취소">
    </div>


  <%@include file ="/WEB-INF/views/layout/footer.jsp" %>
  <script src="/resources/js/login/changePw.js"></script>
