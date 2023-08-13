<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="CP" value="${pageContext.request.contextPath }"/>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta name="author"  content="hbi">  
<!-- CSS -->
<link  rel="stylesheet" href="${CP}/resources/css/default.css" >
<link  href="${CP}/resources/css/bootstrap/bootstrap.min.css" rel="stylesheet"  crossorigin="anonymous">
<script src="${CP}/resources/js/bootstrap/bootstrap.bundle.min.js"  crossorigin="anonymous"></script>
<script src="${CP}/resources/js/jquery-3.7.0.js"></script>
<script type="text/javascript"> 
   history.replaceState({}, null, location.pathname); 
</script> 
<link>
<title>로드스캐너 ID & PW찾기 </title>
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
      </ul>
      <form class="d-flex" role="search">
        <!-- 로그인 세션 X -->
        <c:if test="${user eq null}">
          <button type="button" id="login" onclick="location.href='${CP}/login'" class="btn btn-outline-primary me-2">Login</button>
        </c:if>
        <!-- 로그인 세션 O -->
        <c:if test="${user ne null}">
          <button type="button" class="btn btn-outline-primary me-2" onclick="location.href='${CP}/mypage'">MyPage</button>
          <button type="button" class="btn btn-outline-primary me-2" onclick="location.href='${CP}/logout'">LogOut</button>
        </c:if>
           <!-- 관린자 -->
           <c:if test="${user.grade ==2}">
               <button type="button" class="btn btn-outline-primary" onclick="location.href='${CP}/admin'" style="margin-right: 50px;">관리자</button>
           </c:if>
          <button type="button" onclick="location.href='${CP}/registerpage'" class="btn btn-outline-primary" style="margin-right: 50px;">Sign-up</button>
      </form>
    </div>
  </div>
</nav>
<body class="d-flex flex-column min-vh-100">
<div style="margin-top: 70px;">
      <div class = "roadscannercontainer"><!-- id 찾기 -->
        <h1 style="text-align: center; font-weight: 900;">RoadScanner</h1>
            <h4 style="text-align: center; margin-top:50px; font-weight: 800;">아이디 찾기</h4>
        <form  class = "formabc" onsubmit="return false;">
                이메일<br/>
          <input type="email" class = "findinput" id="email" name= "email" placeholder="이메일"><br/>
          <input type="hidden" id="set_id">
          <button type="button" class= "findbtn" id="findId" name="findId">아이디 찾기</button>
        </form>
        <input type ="hidden" id ="id" name ="id">
      </div><!-- id 찾기 -->
      <div class = "jb-division-line"></div>
	  <div class = "roadscannercontainer"><!-- pw 찾기 -->
	  <h4 style="text-align: center; margin-bottom:50px; font-weight: 800;">비밀번호 찾기</h4>
        <form class = "formabc" onsubmit="return false;">
          아이디<br/>
          <input type="text" class="findinput" style="margin-bottom: 20px;"
          id="userId" name="userId" onkeyup="id_form_check(event)" placeholder="아이디"><br/>
          이메일<br/>
          <input type="email" class="findinput"  id="email2" name= "email2" placeholder="이메일"><br/>
          <input type="hidden" id="set_pw">
          <button type="button" class= "findbtn" id="findPw" name="findPw">비밀번호 찾기</button>
        </form>
        
        
        <div 
	      style="text-align: center; width:400px; height: 60px; margin:40px auto;">
	      </div>
      </div><!-- pw 찾기 -->   
</div> <!-- container --> 
 

</body> 
<footer class="py-3 my-4 mt-auto">
    <ul class="nav justify-content-center border-bottom pb-3 mb-3">
    </ul>
    <p class="text-center text-body-secondary">&copy; 2023 F1 RoadScanner Project, All rights reserved.</p>
  </footer>
   <script>
   
   function id_form_check(event) {
       const reg_id = /[^0-9a-z]/g;
       const ele = event.target; 
       
       if(reg_id.test(ele.value)) {
         ele.value = ele.value.replace(reg_id,'');
       }
       
     }   
   
    $(document).ready(function(){ //모든 화면이 다 로딩이 되면 실행하는 영역
    	   	
        $("#findId").on("click",function(){ 
          
          console.log("email : "+$("#email").val());
            
          
          if(""==$("#email").val() || 0==$("#email").val().length){
              alert("이메일을 입력하세요");  // javascript 메시지 다이얼 로그
              $("#email").focus();
              return;
            }
          
          $.ajax({
                type: "POST",
              url:"${CP}/findId",
                /* asyn:"true", */
                dataType:"html",
                data:{
                    email: $("#email").val()
                },
                success:function(data){//통신 성공
                    //console.log("success data:"+data);
                    // JSON.parse() 메서드는 JSON 문자열의 구문을 분석하고, 그 결과에서 JavaScript 값이나 객체를 생성합니다.
                    let paredJSON = JSON.parse(data);
                    console.log("paredJSON.msgId:"+paredJSON.msgId);
                    
                    if("1"==paredJSON.msgId || "10"==paredJSON.msgId){
                      alert(paredJSON.msgContents);  // javascript 메시지 다이얼 로그
                      return;
                    }
                    if("30"==paredJSON.msgId){ //서치 성공
                      $('#set_id').attr('value',paredJSON.msgContents);
                      email_id_find();
                      
                    }
                    
                  },
                  error:function(data){//실패시 처리
                    console.log("error:"+data);
                  }
            }); //  $.ajax End --------------------------     
          
          
        }); // $("#findId").on("click") End --------------------------
        
        
        
        $("#findPw").on("click",function(){  
            console.log("userId : "+$("#userId").val());
            console.log("email2  : "+$("#email2").val());

            if(""==$("#userId").val() || 0==$("#userId").val().length){
              alert("아이디를 입력하세요");  
              $("#userId").focus();      
              return;
            }
            if(""==$("#email2").val() || 0==$("#email2").val().length){
              alert("이메일을 입력하세요");  
              $("#email2").focus();      
              return;
            }
           
            $.ajax({
                  type: "POST",
                  url:"${CP}/findPw",
                  /* asyn:"true", */
                  dataType:"html",
                  data:{
                    id   : $("#userId").val(),
                    email : $("#email2").val(),
                    
                  },
                  success:function(data){//통신 성공
                      //console.log("success data:"+data);
                      // JSON.parse() 메서드는 JSON 문자열의 구문을 분석하고, 그 결과에서 JavaScript 값이나 객체를 생성합니다.
                      let paredJSON = JSON.parse(data);
                      console.log("paredJSON.msgId:"+paredJSON.msgId);
                      
                      if("1"==paredJSON.msgId || "10"==paredJSON.msgId){
                        alert(paredJSON.msgContents);  
                        return;
                      }
                      if("30"==paredJSON.msgId){//로그인 성공
                    	$('#set_pw').attr('value',paredJSON.msgContents);
                    	email_pw_find();
                    	
                      }
                    },
                    error:function(data){//실패시 처리
                      console.log("error:"+data);
                    }
              }); //  $.ajax End --------------------------       
          }); // $("#findPw").on("click") End --------------------------             
      }); // $(document).ready End --------------------------
      
      function email_id_find() {
    	  
    	   const email = $("#email").val();
    	   const id = $("#set_id").val();
    	  
    	  $.ajax({
              type : 'POST',
              url : "toEmailFindId?email=" + email + "&id=" + id 
              
          }); // end ajax
          alert('요청하신 이메일로 아이디를 보내드렸습니다.');
    	  $('#email').val('');
      }
      
      function email_pw_find() {
          
          const email = $("#email2").val();
          const pw = $("#set_pw").val();
         
         $.ajax({
             type : 'POST',
             url : "toEmailFindPw?email=" + email + "&pw=" + pw 
             
         }); // end ajax
         alert('요청하신 이메일로 비밀번호를 보내드렸습니다.');
         $('#userId').val('');
         $('#email2').val('');
     }
    
    </script>
</html>