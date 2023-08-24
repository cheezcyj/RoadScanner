  <%@include file ="head.jsp" %>

<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<link  rel="stylesheet" href="${CP}/resources/css/default.css" >
<script type="text/javascript"> 
   history.replaceState({}, null, location.pathname); 
</script> 
<title>로드스캐너 ID & PW찾기 </title>

  <%@include file ="navbar.jsp" %>

<body id="font-id" class="d-flex flex-column min-vh-100">
<div style="margin-top: 70px;">
      <div class = "roadscannercontainer"><!-- id 찾기 -->
      
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

</body> 

  <%@include file ="footer.jsp" %>

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
                  
                    
                    if("1"==paredJSON.msgId || "10"==paredJSON.msgId){
                      alert(paredJSON.msgContents);  // javascript 메시지 다이얼 로그
                      return;
                    }
                    if("30"==paredJSON.msgId){ //서치 성공
                    	$('#set_id').attr('value', paredJSON.msgContents);
                    	email_id_find();
                      
                    }
                    
                  },
                  error:function(data){//실패시 처리
                    console.log("error:"+data);
                  }
            }); //  $.ajax End --------------------------     
          
          
        }); // $("#findId").on("click") End --------------------------
        
        
        
        $("#findPw").on("click",function(){  
         

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
                     
                      if("1"==paredJSON.msgId || "10"==paredJSON.msgId){
                        alert(paredJSON.msgContents);  
                        return;
                      }
                      if("30"==paredJSON.msgId){
                    	  window.location.href="${CP}/changePw";
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
              url : "/login/toEmailFindId?email=" + email + "&id=" + id 
              
          }); // end ajax
          alert('요청하신 이메일로 아이디를 보내드렸습니다.');
    	  $('#email').val('');
      }
    
</script>
</html>