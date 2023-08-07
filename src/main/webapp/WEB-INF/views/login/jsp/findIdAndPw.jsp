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
<link  href="${CP}/resources/css/bootstrap/bootstrap.min.css" rel="stylesheet"  crossorigin="anonymous">
<script src="${CP}/resources/js/bootstrap/bootstrap.bundle.min.js"  crossorigin="anonymous"></script>
<script src="${CP}/resources/js/jquery-3.7.0.js"></script>
<link>
<title>Find_ID_PW</title>
  </head>
<body>
 <div class = "container">
    <a href="${CP}/login">로그인 페이지</a>
      <div><!-- id 찾기 -->
        <form>
          <input type="email" id="email" name= "email" placeholder="이메일">
          <button type="button" id="findId" name="findId">아이디 찾기</button>
        </form>
        <input type ="hidden" id ="id" name ="id">
      </div><!-- id 찾기 -->
      
	  <div><!-- pw 찾기 -->
        <form>
          <input type="text" id="userId" name="userId" placeholder="아이디">
          <input type="email" id="email2" name= "email2" placeholder="이메일">
          <button type="button" id="findPw" name="findPw">비밀번호 찾기</button>
        </form>
      </div><!-- pw 찾기 -->  
    </div><!-- container -->   
</body> 
   <script>
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
              url:"${path}/findId",
                /* asyn:"true", */
                dataType:"html",
                data:{
                  
                    uemail: $("#email").val()
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
                      alert(paredJSON.msgContents);
                      const id = paredJSON.msgContents.split(' ');
                      console.log("id:"+id[1]);
                      $('#userId').attr('value',id[1]);
                      $('#email2').attr('value',$('#email').val());
                      $('#email').val('');
                      
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
              alert("이름을 입력하세요");  
              $("#email2").focus();      
              return;
            }
           
            $.ajax({
                  type: "POST",
                  url:"${path}/findPw",
                  /* asyn:"true", */
                  dataType:"html",
                  data:{
                    uid   : $("#userId").val(),
                    uemail : $("#email2").val(),
                    
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
                        alert(paredJSON.msgContents);
                        
                      }
                    },
                    error:function(data){//실패시 처리
                      console.log("error:"+data);
                    }
              }); //  $.ajax End --------------------------       
          }); // $("#findPw").on("click") End --------------------------             
      }); // $(document).ready End --------------------------
    
    </script>
</html>