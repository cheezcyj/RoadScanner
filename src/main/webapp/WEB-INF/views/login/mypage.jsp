  <%@include file ="/WEB-INF/views/layout/header.jsp" %>
  
<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib  prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
    // 서버 측에서 사용자 세션을 확인하고, 세션이 없으면 기본 페이지로 리다이렉트합니다.
    if (session.getAttribute("user") == null) {
      response.sendRedirect("/login");
    }
%>
<!-- CSS -->
<link rel="stylesheet"  href="${CP}/resources/css/mypage.css">

  <%@include file ="/WEB-INF/views/layout/navbar.jsp" %>

<body id="font-id" class="d-flex flex-column" style=" min-height: 80vh !important;">
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
			    <input type="button" class="btn btn-secondary" id="cancle"  value="취소">    
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
  
  <%@include file ="/WEB-INF/views/layout/footer.jsp" %>
  <script src="/resources/js/login/mypage.js"></script>
