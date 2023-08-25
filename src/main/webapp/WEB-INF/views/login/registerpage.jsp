<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="CP" value="${pageContext.request.contextPath }"/>  
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<!-- CSS -->
<link  href="${CP}/resources/css/membership-style.css" rel="stylesheet">
<link  href="${CP}/resources/css/bootstrap/bootstrap.min.css" rel="stylesheet"  crossorigin="anonymous">
<script src="${CP}/resources/js/bootstrap/bootstrap.bundle.min.js"  crossorigin="anonymous"></script>
<script src="${CP}/resources/js/jquery-3.7.0.js"></script>
<title>로드스캐너 회원가입</title>
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
          <button type="button" id="login" onclick="location.href='${CP}/login'" class="btn btn-outline-primary" style="margin-right: 50px;">Login</button>
        </c:if>
        <!-- 로그인 세션 O -->
        <c:if test="${user ne null}">
          <button type="button" class="btn btn-outline-primary me-2" onclick="location.href='${CP}/mypage'">MyPage</button>
          <button type="button" class="btn btn-outline-primary" onclick="location.href='${CP}/logout'" style="margin-right: 50px;">LogOut</button>
        </c:if>
      </form>
    </div>
  </div>
</nav>
<body class="d-flex flex-column min-vh-100">
<div class ="reg_container">
    <h1>RoadScanner 회원가입</h1>
    &nbsp;
	       <form action="" method="post" name="membership" style="margin: auto;">         
	           <ul class= "ccu">
	             <li class="cc">
	             
	               <label for= "id_form" style="float: left">아이디</label><br/>
	             <div style="display: flex">
	               <input type="text" name="id_form" id="id_form" onkeyup="id_form_check(event)" onchange="id_length_check()" placeholder="아이디 입력(영어, 숫자포함 6~20자)">
	               <input type="button" class = "btn btn-outline-dark" id="idDulpCheck" value="중복확인"
	               style="margin-left: 15px;">
	             </div>
	             </li>
	             <li class="cc">
	               <label style="float: left">비밀번호</label><br/>
	               <input type="password" name="pw_form" id="pw_form" placeholder="(문자, 숫자, 특수문자[!,@,#,$,%,*]) 포함 8~20자)" onchange="check_pw()"
	               style="float: left;"><br/>
	               
	             </li>
	             <li class="cc">
	               <label style="float: left; ">비밀번호 확인</label><label id="pw_check"></label><br/>
	               <input type="password" name="pw2_form" id="pw2_form" placeholder="비밀번호 재입력" onchange="check_pw()"
	               style="float: left;"><br/>
	                        
	             </li>
	             <li class="cc">
		             <div>
		               <label style="float: left">이메일 주소</label><br/>
		               <div style="display: flex">
		               <input type="email" name="email_front" id="email_front" onkeyup="check_email(event)" placeholder="이메일 주소">
		               <label style="width:30px;">@</label>
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
		               <input type="button" class="btn btn-outline-dark" id="emailDulpCheck" value="중복확인"
		                style="margin-left: 15px;">
		               </div>
		             </div>
	             </li>
	             
	             

	             <li class="cc">
	              
		            <label for= "mail-Check-Btn" style="float: left">E-mail 인증번호 입력</label><br/>
		            <div style="display: flex">
		             <input type="button" class = "btn btn-outline-dark nest1" 
                           id="mail-Check-Btn" value="인증번호 전송 "
                           class="nest1" >
                           
	                  <input type="text" class="emailcheck" name="checkInput" id="checkInput"
	                   placeholder="인증번호 6자리를 입력해주세요" maxlength="6" required
	                   style="margin-left: 16px;"><br/>
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
	</form>
	<input type="hidden" name="emailok" id="emailok"> 
	<input type="hidden" name="idok" id="idok"> 
</div>    
</body>
<footer class="py-3 my-4 mt-auto">
  <ul class="nav justify-content-center border-bottom pb-3 mb-3">
  </ul>
  <p class="text-center text-body-secondary">&copy; 2023 F1 RoadScanner Project, All rights reserved.</p>
</footer>
    <script>
       
        function check_pw() {
            var pw = document.getElementById('pw_form').value;
            var num = pw.search(/[0-9]/g);
            var eng = pw.search(/[a-z]/ig);
            var spe = pw.search(/[`~!@@#$%^&*|₩₩₩'₩";:₩/?]/gi);
 
            if(pw.length<8 || pw.length>20) {
               window.alert('비밀번호는 8글자 이상, 20글자 이하만 이용 가능합니다.');
               document.getElementById('pw_form').value='';
            } else if(pw.search(/\s/) != -1) {
               window.alert('비밀번호는 공백 없이 이용 가능합니다');
               document.getElementById('pw_form').value='';
            } else if(num < 0 || eng < 0 || spe < 0) {
               window.alert('영문, 숫자, 특수문자를 최소 1글자 이상씩 사용하여 입력해주세요');
               document.getElementById('pw_form').value='';
            }           
            
            if(document.getElementById('pw_form').value !='' && document.getElementById('pw2_form').value!='') {
              
                if(document.getElementById('pw_form').value == document.getElementById('pw2_form').value) {
                    document.getElementById('pw_check').innerHTML='비밀번호가 일치합니다.'
                    document.getElementById('pw_check').style.color='blue';
                } else {
                    document.getElementById('pw_check').innerHTML='비밀번호가 일치하지 않습니다.';
                    document.getElementById('pw_check').style.color='red';
                    document.getElementById('pw_form').value='';
                    document.getElementById('pw2_form').value='';
                }
                
            }
        }
        
        function id_form_check(event) {
            const reg_id = /[^0-9a-z]/g;
            const ele = event.target; 
            
            if(reg_id.test(ele.value)) {
              ele.value = ele.value.replace(reg_id,'');
            }
            
          }
      
       function id_length_check() {
           const registerId = document.getElementById('id_form').value;
           
           if(registerId.length < 6 || registerId.length > 20) {
                 alert("아이디는 6~20글자로 구성되어야 합니다");
                 document.getElementById('id_form').value='';
            }
           
         }
    
      function check_email(event) {
         const hangul = /[^0-9a-zA-Z]/g;
         const ele = event.target;
         
         if(hangul.test(ele.value)) {
           ele.value = ele.value.replace(hangul,'');
         }
       }
      
      /* 취소 버튼 클릭 시 첫 화면으로 이동 / href="메인페이지 주소 입력하기" */
      /* function goFirstForm() {
          location.href="MainForm.do";
      } */
      
      $(document).ready(function(){  //모든 화면이 다 로딩이 되면 실행하는 영역
            console.log("document ready");
      
        $("#noneRegister").on("click", function(){
            alert("회원가입을 취소했습니다");
            window.location.href="${CP}/login";
            
        });
      
        $("#register").on("click", function(){
            console.log("register ready");

            let registerId = document.getElementById('id_form').value;
            let registerPw = document.getElementById('pw2_form').value;
            let registerEmail =  $('#email_front').val()+"@"+ $('#email_back').val()
            console.log(registerEmail);
           
            
            document.register_form.id.value = registerId;
            document.register_form.pw.value = registerPw;
            document.register_form.email.value = registerEmail;
            
            
         
            if("" == document.getElementById('id_form').value) {
                alert("아이디를 입력하세요");
                return false;
            }
            
            if("ok" != document.getElementById('idok').value) {
                alert("아이디 인증을 진해하십시오.");
                return false;
            }
            
            if("" == document.getElementById('pw_form').value || "" == document.getElementById('pw2_form').value) {
                alert("비밀번호를 입력하세요");
                return false;
            }
                  
            if("" == document.getElementById('email_front').value || "" == document.getElementById('email_back').value) {
                alert("이메일을 입력하세요");
                return false;
            }
            if("" == document.getElementById('checkInput').value){
            	alert("이메일 인증을 진행해 주십시오.");
                return false;
            }
            
            if( document.getElementById('checkInput').value != code){
                alert("이메일 인증번호가 다릅니다.");
                return false;
            }
                      
            $.ajax({
                type: "POST",
              url:"${CP}/register",
                /* asyn:"true", */
                dataType:"html",
                data:{
                	grade: $("#grade").val(),
                    id: $("#id").val(),
                    password: $("#pw").val(),
                    email: $("#email").val()
                },
                success:function(data){//통신 성공
                    //console.log("success data:"+data);
                    // JSON.parse() 메서드는 JSON 문자열의 구문을 분석하고, 그 결과에서 JavaScript 값이나 객체를 생성합니다.
                    let parsedJSON = JSON.parse(data);
                    console.log("parsedJSON.msgId:"+parsedJSON.msgId);
                                     
                    
                    if("10" == parsedJSON.msgId){
                      alert(parsedJSON.msgContents);
                      window.location.href="${CP}/login";
                    } 
                    
                    if("20" == parsedJSON.msgId){
                      alert(parsedJSON.msgContents);
                      return;
                    }
                    
                   
                  },
                  error:function(data){//실패시 처리
                    console.log("error:"+data);
                  }
            }); //  $.ajax End --------------------------
            
            
        });    // #register END
            
        //jquery 이벤트 감지 (#은 id를 감지하는 것이다.)
        $("#idDulpCheck").on("click",function(){
            console.log("idDulpCheck ready");
            
            var id_str = document.getElementById('id_form').value;
            console.log(id_str);
            
            if(""==$('#id_form').val() || 0==$('#id_form').val().length){
              alert("아이디를 입력하세요");  // javascript 메시지 다이얼 로그
              $('#id_form').focus();          // jquery로 포커스를 이동시킨다.
              return;
          } else if(id_str.search(/\s/) != -1) {
              alert('아이디는 공백 없이 입력 가능합니다');
              document.getElementById('id_form').value='';
              return;
          }
            
            $.ajax({
                type: "POST",
              url:"${CP}/idDulpCheck",
                /* asyn:"true", */
                dataType:"html",
                data:{
                	id: $("#id_form").val()
                },
                success:function(data){//통신 성공
                    //console.log("success data:"+data);
                    // JSON.parse() 메서드는 JSON 문자열의 구문을 분석하고, 그 결과에서 JavaScript 값이나 객체를 생성합니다.
                    let parsedJSON = JSON.parse(data);
                    console.log("parsedJSON.msgId:"+parsedJSON.msgId);
                                     
                    
                    if("10" == parsedJSON.msgId){
                      alert(parsedJSON.msgContents);  // javascript 메시지 다이얼 로그
                      $("#id_form").focus();
                    } 
                    
                    if("20" == parsedJSON.msgId){//로그인 성공
                      alert(parsedJSON.msgContents);
                      $('#idok').attr('value',"ok");
                      return;
                    }
                    
                   
                  },
                  error:function(data){//실패시 처리
                    console.log("error:"+data);
                  }
            }); //  $.ajax End --------------------------
            
            
        });  // #idDulpCheck end
        
        $("#emailDulpCheck").on("click",function(){
            console.log("emailDulpCheck ready");
            
            var emial_str = $('#email_front').val()+"@"+ $('#email_back').val()
            console.log(emial_str);
            
            if(""==$('#email_front').val() || 0==$('#email_front').val().length){
              alert("이메일 앞자리를 입력하세요");  // javascript 메시지 다이얼 로그
              $('#email_front').focus();          // jquery로 포커스를 이동시킨다.
              return;
              
          } if(""==$('#email_back').val() || 0==$('#email_front').val().length){
              alert("이메일 뒷자리를 입력하세요");  // javascript 메시지 다이얼 로그
              $('#email_back').focus();          // jquery로 포커스를 이동시킨다.
              return;
              
          } else if(emial_str.search(/\s/) != -1) {
              alert('이메일은 공백 없이 입력하도록');
              document.getElementById('email_front').value='';
              document.getElementByName('email_back')[0].value='';
              return;
          }
          
          $.ajax({
              type: "POST",
            url:"${CP}/emailDulpCheck",
              /* asyn:"true", */
              dataType:"html",
              data:{
            	  email: emial_str
              },
              success:function(data){//통신 성공
                  //console.log("success data:"+data);
                  // JSON.parse() 메서드는 JSON 문자열의 구문을 분석하고, 그 결과에서 JavaScript 값이나 객체를 생성합니다.
                  let parsedJSON = JSON.parse(data);
                  console.log("parsedJSON.msgId:"+parsedJSON.msgId);
                                   
                  
                  if("10" == parsedJSON.msgId){
                    alert(parsedJSON.msgContents);  // javascript 메시지 다이얼 로그
                    $("#email_front").focus();
                  } 
                  
                  if("20" == parsedJSON.msgId){//
                    alert(parsedJSON.msgContents);
                    $('#emailDulpCheckbool').attr('value',$('#email').val());
                    $('#emailDulpCheckbool').attr('value',$('#email').val());
                    $('#emailok').attr('value',"ok");
                    return;
                  }
                  
                 
                },
                error:function(data){//실패시 처리
                  console.log("error:"+data);
                }
          }); //  $.ajax End --------------------------
            
            
            
        });  // #idDulpCheck end
 
     }); 
</script>
<script>
$('#mail-Check-Btn').click(function() {
    const email = $('#email_front').val()+"@"+ $('#email_back').val() // 이메일 주소값 얻어오기!
    console.log($('#email').val());
    const checkInput = $('#checkInput'); // 인증번호 입력하는곳
    if ("" == document.getElementById('email_front').value || "" == document.getElementById('email_back').value) {
        alert('이메일을 입력하십시오.');
        email_Check = false;
        $("#email").focus();
    }else if("ok" != document.getElementById('emailok').value){
    	alert('이메일을 중복확인후에 진행하시오.');
    }else {
        $.ajax({
            type : 'get',
            url : "mailCheck?email=" + email, // GET방식이라 Url 뒤에 email을 뭍힐수있다.
            success : function(data) {

                console.log("data : " + data);
                $('#checkInput').attr('disabled', false);
                code = data;
                alert('인증번호가 전송되었습니다.')
            }
        }); // end ajax
    }

}); // end send eamil

// 인증번호 비교
// blur -> focus가 벗어나는 경우 발생
$('#checkInput').blur(function() {
    const inputCode = $(this).val();
    const $resultMsg = $('#mail-check-warn');

    if (inputCode == code) {
        $resultMsg.html('인증번호가 일치합니다.');
        $resultMsg.css('color', 'green');
        $resultMsg.css('display', 'block');
        $resultMsg.css('font-size', '13px');
        $('#mail-Check-Btn').attr('disabled', true);
        $('#email').attr('readonly', true);
        $('#register').attr('disabled', false);
        certified_Email = true;
    } else {
        certified_Email = false;
        $resultMsg.html('인증번호가 불일치 합니다. 다시 확인해주세요!');
        $resultMsg.css('color', 'red');
        $resultMsg.css('display', 'block');
        $resultMsg.css('font-size', '13px');
    }
});

</script>
</html>