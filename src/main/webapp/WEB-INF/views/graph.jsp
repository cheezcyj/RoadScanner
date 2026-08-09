<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib  prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageStylesheet" value="/resources/css/graph.css?v=2" />
<%@include file ="/WEB-INF/views/layout/header.jsp" %>

<body class="rs-internal-page d-flex flex-column min-vh-100">
  <%@include file ="/WEB-INF/views/layout/navbar.jsp" %>
  <div class="container main-content">
    <h2>Feedback</h2>
    
    <div class="accordion">
		  <div class="accordion-item">
		    <div class="accordion-header">
		      <span>싫어요 피드백 누적 개수</span>
		      <span class="accordion-icon" aria-hidden="true">⌄</span>
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
          <span class="accordion-icon" aria-hidden="true">⌄</span>
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
  
	<script src="${CP}/webjars/chart.js/3.9.1/dist/chart.min.js"></script>
  <script src="${CP}/resources/js/graph.js?v=2"></script>
  <%@include file ="/WEB-INF/views/layout/footer.jsp" %>
