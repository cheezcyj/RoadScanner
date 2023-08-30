
//비밀번호 정규식
function check_pw() {
    var pw = document.getElementById('rpassword').value;
    var num = pw.search(/[0-9]/g);
    var eng = pw.search(/[a-z]/ig);
    var spe = pw.search(/[`~!@@#$%^&*|₩₩₩'₩";:₩/?]/gi);
       
    if(pw.length<8 || pw.length>20) {
       window.alert('비밀번호는 8글자 이상, 20글자 이하만 이용 가능합니다.');
       document.getElementById('rpassword').value='';
       document.getElementById('rpassword').focus();
    } else if(pw.search(/\s/) != -1) {
       window.alert('비밀번호는 공백 없이 이용 가능합니다');
       document.getElementById('rpassword').value='';
       document.getElementById('rpassword').focus();
    } else if(num < 0 || eng < 0 || spe < 0) {
       window.alert('영문, 숫자, 특수문자를 최소 1글자 이상씩 사용하여 입력해주세요');
       document.getElementById('rpassword').value='';
       document.getElementById('rpassword').focus();
    }           
    
    if(document.getElementById('rpassword').value !='' && document.getElementById('rpassword2').value!='') {
      
        if(document.getElementById('rpassword').value == document.getElementById('rpassword2').value) {
            document.getElementById('pw_check').innerHTML='비밀번호가 일치합니다.'
            document.getElementById('pw_check').style.color='blue';
            document.getElementById('pw_check').style.fontSize='15px';
        } else {
            document.getElementById('pw_check').innerHTML='비밀번호가 일치하지 않습니다.';
            document.getElementById('pw_check').style.color='red';
            document.getElementById('pw_check').style.fontSize='15px';
            document.getElementById('rpassword').value='';
            document.getElementById('rpassword2').value='';
        }
        
    }
    
}   // check_pw end

// 이메일 정규식
function check_email(event) {
    const eng = /[^0-9a-z@\.]/g;
    const ele = event.target;
    
    if(eng.test(ele.value)) {
      ele.value = ele.value.replace(eng,'');
    }
    
}   // check_email

// 이메일 인증번호
function email_authNumber() {
    const email = $('#remail').val();    // 이메일 주소값 얻어오기!
    const checkInput = $('#checkInput'); // 인증번호 입력하는곳
        
    if("ok" != document.getElementById('emailok').value){
      
      alert('이메일 중복 확인 후, 진행하세요');
      
    } else {
      
        $.ajax({
            type : 'get',
            url : "change_mailCheck?email=" + email,
            success : function(data) {

                console.log("data : " + data);
                $('#checkInput').attr('disabled', false);
                code = data;
                alert('인증번호가 전송되었습니다.');
            }
        
        }); // end ajax
        
    }   // else end

} // email_authNumber end

// 이메일 인증번호 비교 (blur -> focus가 벗어나는 경우 발생)
$('#checkInput').blur(function() {
    const inputCode = $(this).val();
    const $resultMsg = $('#mail-check-warn');

    if (inputCode == code) {
        $resultMsg.html('인증번호가 일치합니다.');
        $resultMsg.css('color', 'blue');
        $resultMsg.css('font-size', '15px');
        $('#mail-Check-Btn').attr('disabled', true);
        $('#remail').attr('readonly', true);
        certified_Email = true;
    } else {
        certified_Email = false;
        $resultMsg.html('인증번호가 일치하지 않습니다.');
        $resultMsg.css('color', 'red');
        $resultMsg.css('font-size', '15px');
    }
    
});   // checkInput function end

$(document).ready(function(){
   console.log("document ready");
   
  $("#cancle").on("click", function(){
    
    window.location.href='/findIdPw';
    
  });   // $("#cancel") click
  
  $("#changePw").on("click", function(){
    
      let registerPw = document.getElementById('rpassword2').value;
      let registerEmail =  $('#remail').val();
      console.log(registerEmail);
       
      document.register_form.pw.value = registerPw;
      document.register_form.email.value = registerEmail;
      
      
      if("" == document.getElementById('remail').value){
          alert("이메일 인증을 진행해주십시오.");
            return false;
        }
            
      if("" == document.getElementById('checkInput').value){
        alert("이메일 인증번호를 입력해주십시오.");
          return false;
      }

      if("" == document.getElementById('rpassword').value || "" == document.getElementById('rpassword2').value) {
          alert("비밀번호를 입력하세요");
          return false;
      }
      
      if( document.getElementById('checkInput').value != code){
          alert("이메일 인증번호가 다릅니다.");
          return false;
      }
    
    $.ajax({
          type: "POST",
          url:"/changePassword",
          asyn:"true",
          dataType:"html",
          data:{
            password: $("#pw").val(),
            email: $("#email").val()
          },
          success:function(data){
              let parsedJSON = JSON.parse(data);
              
              // 비밀번호 재설정 성공
              if("10" == parsedJSON.msgId) {
                alert(parsedJSON.msgContents);
                window.location.href="/login";
              }
              
              // 비밀번호 재설정 실패
              if("20" == parsedJSON.msgId) {
                  alert(parsedJSON.msgContents);
                  return;
               }
              
            },
            error:function(data){//실패시 처리
              console.log("error:"+data);
            }
        });  // ajax end
    
  });   // $("#changePw") click
  
  $("#emailDulpCheck").on("click",function(){
      console.log("emailDulpCheck ready");
      
      var emial_str = $('#remail').val();
      
      if(""==$('#remail').val()) {
        alert("이메일을 입력하세요");
        $('#remail').focus(); 
        return;
        
    } else if(emial_str.search(/\s/) != -1) {
        alert('이메일은 공백 없이 입력하세요');
        document.getElementById('remail').value='';
        return;
    }
    
    $.ajax({
        type: "POST",
        url:"/emailCheck",
        dataType:"html",
        data:{
          email: emial_str
        },
        success:function(data){
            let parsedJSON = JSON.parse(data);                        
            
            if("20" == parsedJSON.msgId){
              alert(parsedJSON.msgContents);  // javascript 메시지 다이얼 로그
              $("#remail").focus();
            } 
            
            if("10" == parsedJSON.msgId){//
              if (confirm(parsedJSON.msgContents) == true){
              $('#emailok').attr('value',"ok");
              email_authNumber();
              };
            }else{
            	return;
            }
            
           
          },
          error:function(data){//실패시 처리
            console.log("error:"+data);
          }
          
    }); //  $.ajax End --------------------------    
    
  });  // #idDulpCheck end
   
});   //모든 화면이 다 로딩이 되면 실행하는 영역