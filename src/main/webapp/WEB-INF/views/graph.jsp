<header>
  <%@include file ="/WEB-INF/views/layout/header.jsp" %>
 </header>
<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib  prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
    // 서버 측에서 사용자 세션을 확인하고, 세션이 없으면 기본 페이지로 리다이렉트합니다.
    if (session.getAttribute("user") == null) {
      response.sendRedirect("/login");
    }
%>
<!-- CSS -->
<link rel="stylesheet" href="/resources/css/graph.css" >
<script src="https://kit.fontawesome.com/726783c905.js" crossorigin="anonymous"></script>

  <%@include file ="/WEB-INF/views/layout/navbar.jsp" %>

<body id="font-id">
  <div class="container main-content">
    <h2>Feedback</h2>
    
    <div class="accordion">
		  <div class="accordion-item">
		    <div class="accordion-header">
		      <span>싫어요 피드백 누적 개수</span>
		      <span class="accordion-icon"><i class="fa-solid fa-chevron-down fa-lg" id="logo-fix1" style="color: #000000;"></i></span>
		    </div>
		    <div class="accordion-content">
		      <div class="d-flex justify-content-center align-items-center flex-column">
			      <div class="barchart" style="margin-top:20px;">
			        <canvas id="feedback_barchart"></canvas>
			      </div>
			      <table class="table table-bordered text-center" style="width: 60%;">
			        <thead>
			          <tr class="table-secondary">
			            <th>분류</th>
			            <th>누적 개수</th>
			          </tr>
			        </thead>
			        <tbody>
			          <tr>
			            <td>모양 오류</td>
			            <td id="u1"></td>
			          </tr>
			          <tr>
			            <td>색깔 오류</td>
			            <td id="u2"></td>
			          </tr>
			          <tr>
			            <td>그림/숫자 오류</td>
			            <td id="u3"></td>
			          </tr>
			          <tr  class="table-light">
			            <td>전체</td>
			            <td id="total"></td>
			          </tr>
			        </tbody>
			      </table>
			    </div>
		    </div> <!-- accordion-content -->
		  </div> <!-- accordion-item -->
		  <div class="accordion-item">
		    <div class="accordion-header">
          <span>월별 싫어요 피드백 개수 변화</span>
          <span class="accordion-icon"><i class="fa-solid fa-chevron-down fa-lg" id="logo-fix2" style="color: #000000;"></i></span>
		    </div>
		    <div class="accordion-content">
		      <div class="container" style="width:60%; margin-bottom:10px;">
			      <select class="form-select" id="monthDropDown" name="month" style="width:100px;">
			      </select>
			    </div>
			    <div class="linechart">
			      <canvas id="feedback_linechart"></canvas>
			    </div>
		    </div> <!-- accordion-content -->
		  </div> <!-- accordion-item -->
		</div>   
  </div> <!--container -->
  
  <%-- 세션이 없이 해당 페이지 진입하면 로그인 페이지로 이동 --%>
  <c:if test="${user.grade != 2}">
    <script>     
        window.location.href = "/login";
    </script>   
  </c:if>

  <script src="https://code.jquery.com/jquery-3.7.0.min.js"></script>
  <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
	<script src="https://cdn.jsdelivr.net/npm/chart.js@3.5.1"></script>
  <script src="/resources/js/graph.js"></script>
</body>