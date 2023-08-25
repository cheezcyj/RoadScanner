<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib  prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="CP" value="${pageContext.request.contextPath}"/> 
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <!-- CSS -->
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-9ndCyUaIbzAi2FUVXJi0CjmCapSmO7SnpJef0486qhLnuZ2cdeRhO02iuK6FUUVM" crossorigin="anonymous">
  <link rel="stylesheet" href="${CP}/resources/js/fullpage/fullPage.css">
  <link rel="stylesheet" href="${CP}/resources/css/main.css" >
  <link href="https://hangeul.pstatic.net/hangeul_static/css/nanum-square-neo.css" rel="stylesheet">
  
  <!-- JS -->
  <script src="${CP}/resources/js/jquery-3.7.0.js"></script>
  <script src="${CP}/resources/js/fullpage/fullPage.js"></script>
  <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js" integrity="sha384-geWF76RCwLtnZ8qwWowPQNguL3RmwHVBC9FhGdlKrxdiJJigb/j/68SIy3Te4Bkz" crossorigin="anonymous"></script>
  
  <title>RoadScanner</title> 
</head>

<body>

<div id="fullpage">

  <div class="section active">
    <video class="intro_video" src="${CP}/resources/video/test02.mov" data-autoplay loop muted></video>
    <div class="intro_logo">
        <p>RoadScanner</p>
    </div>
  </div>
 
  <div class="section">
    <video class="intro_video" src="${CP}/resources/video/driving.mp4" data-autoplay loop muted></video>
	    <div id="second">
	    
	      <div class="info-section" id="third">
	        <b>교통표지판 인식 AI 웹 서비스</b>
	        <br/>
	        
	        <div id="fourth">
	          <br/><br/><br/>
	          <b>'RoadScanner'는 약 40,000개의 훈련 이미지를 활용하여 학습하였고,</b><br/>
	          <b>13,000개의 테스트 이미지를 활용하여 약 98% 정확도를 나타내는 모델을 제작했습니다</b>
	        </div>
	      </div>
	      
	    </div>
    
  </div>
  
  <div class="section">
   <div class="section-button" id="section-button" style="background-image: url('resources/picture/start.jpg');">
    <c:if test="${user eq null}">
      <a id="start-button" href="${CP}/login"><b>START</b></a>
    </c:if>
    <c:if test="${user ne null}">
      <a id="start-button" href="${CP}/main/preUpload"><b>START</b></a>
    </c:if>
   </div>
  </div>
  
</div>
<script>

$(document).ready(function(){
  
  $('#fullpage').fullpage({   
    autoScrolling:true,
    scrollHorizontally: true,
    slidesNavigation : true,
    navigation : true
        
  });
  
});

</script>
</body>
</html>