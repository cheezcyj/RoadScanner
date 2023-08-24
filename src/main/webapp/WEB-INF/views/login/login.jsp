  <%@include file ="head.jsp" %>

<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- CSS -->
<link  rel="stylesheet" href="${CP}/resources/css/default.css">
<title>로드스캐너 로그인</title>

<body id="font-id" class="d-flex flex-column min-vh-100">
<c:if test="${user eq null}"> 
   <a href="${CP}/main" id="head-logo" style="font-weight: 900;">RoadScanner</a>
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
</body>

  <%@include file ="footer.jsp" %>

<script>
    //jquery 이벤트 감지 (#은 id를 감지한는것이다.)
    $("#doLogin").on("click",function(){
      
      
      if(confirm('로그인 하시겠습니까?')==false) return;
        
      if(""==$("#id").val() || 0==$("#id").val().length){
          alert("아이디를 입력하세요");  // javascript 메시지 다이얼 로그
          $("#id").focus();          // jquery로 포커스를 이동시킨다.
          return;
      }
      if(""==$("#pd").val() || 0==$("#pw").val().length){
        alert("비밀번호를 입력하세요");  // javascript 메시지 다이얼 로그
        $("#pw").focus();
        return;
      }
      
      $.ajax({
            type: "POST",
            url:"${CP}/login",
            /* asyn:"true", */
            dataType:"html",
            data:{
              id: $("#id").val(),
              password: $("#pw").val()
            },
            success:function(data){//통신 성공
                //console.log("success data:"+data);
                // JSON.parse() 메서드는 JSON 문자열의 구문을 분석하고, 그 결과에서 JavaScript 값이나 객체를 생성합니다.
                let paredJSON = JSON.parse(data);
            
                
                if("1"==paredJSON.msgId || "10"==paredJSON.msgId){
                  alert(paredJSON.msgContents);  // javascript 메시지 다이얼 로그
                  $("#id").focus();          // jquery로 포커스를 이동시킨다.
                  return;
                }
                if("2"==paredJSON.msgId || "20"==paredJSON.msgId){
                  alert(paredJSON.msgContents);
                  $("#pw").focus();
                  return;
                }
                if("30"==paredJSON.msgId){//로그인 성공
                  window.location.href="${CP}/main/preUpload";
                }
                if("40"==paredJSON.msgId) {
                	alert(paredJSON.msgContents);
                	document.getElementById('id').value='';
                	document.getElementById('pw').value='';
                }
              },
              error:function(data){//실패시 처리
                console.log("error:"+data);
              }
          });
    });    
</script>
</html>