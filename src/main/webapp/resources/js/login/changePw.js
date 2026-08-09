let emailVerified = false;
let emailVerificationToken = null;

function resetPasswordEmailVerification() {
	emailVerified = false;
	emailVerificationToken = null;
	$('#remail').attr('readonly', false);
	$('#emailDulpCheck').attr('disabled', false);
	$('#checkInput').attr('disabled', false).val('');
}

//비밀번호 정규식
function check_pw() {
    var passwordInput = document.getElementById('rpassword');
    var pw = passwordInput.value;
    if (pw && !window.RoadScannerCredentialPolicy.isValidPassword(pw)) {
        window.alert(window.RoadScannerCredentialPolicy.passwordMessage);
        passwordInput.value = '';
        passwordInput.focus();
        return false;
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
    return pw === '' || document.getElementById('rpassword2').value === ''
        || pw === document.getElementById('rpassword2').value;
}   // check_pw end

// 이메일 정규식
function check_email(event) {
	const eng = /[^0-9a-z@\.]/gi;
    const ele = event.target;
    
    if(eng.test(ele.value)) {
      ele.value = ele.value.replace(eng,'');
    }

	emailVerified = false;
	emailVerificationToken = null;
	$('#emailok').val('');
	$('#mail-check-warn').empty();
    
}   // check_email

// 이메일 인증번호
function email_authNumber() {
    const email = $('#remail').val();    // 이메일 주소값 얻어오기!
        
    if("ok" != document.getElementById('emailok').value){
      
      alert('이메일 중복 확인 후, 진행하세요');
      
    } else {
      
        $.ajax({
            type : 'POST',
            url : "change_mailCheck",
			dataType : 'json',
			data : { email: email },
            success : function(data) {
				if ("10" !== data.msgId) {
					alert(data.msgContents);
					return;
				}

                $('#checkInput').attr('disabled', false).val('').focus();
				emailVerified = false;
				emailVerificationToken = null;
                alert(data.msgContents);
			},
			error : function() {
				alert('인증번호 전송에 실패했습니다. 잠시 후 다시 시도해주세요.');
            }
        
        }); // end ajax
        
    }   // else end

} // email_authNumber end

// 이메일 인증번호 비교 (blur -> focus가 벗어나는 경우 발생)
$('#checkInput').blur(function() {
    const inputCode = $(this).val();
    const $resultMsg = $('#mail-check-warn');

	if (!/^\d{6}$/.test(inputCode)) {
		emailVerified = false;
		$resultMsg.html('인증번호 6자리를 입력해주세요.').css({color: 'red', fontSize: '15px'});
		return;
	}

	$.ajax({
		type: 'POST',
		url: 'change_mailCheck/verify',
		dataType: 'json',
		data: { email: $('#remail').val(), code: inputCode },
		success: function(data, textStatus, jqXHR) {
			const receivedToken = jqXHR.getResponseHeader('X-Email-Verification-Token');
			emailVerified = data.msgId === '10' && !!receivedToken;
			emailVerificationToken = emailVerified ? receivedToken : null;
			$resultMsg.html(data.msgContents).css({
				color: emailVerified ? 'blue' : 'red',
				fontSize: '15px'
			});
			if (emailVerified) {
				$('#remail').attr('readonly', true);
				$('#emailDulpCheck').attr('disabled', true);
				$('#checkInput').attr('disabled', true);
			}
		},
		error: function() {
			emailVerified = false;
			emailVerificationToken = null;
			$resultMsg.html('인증 확인에 실패했습니다. 다시 시도해주세요.').css('color', 'red');
		}
	});
});   // checkInput function end

$(document).ready(function(){
  $("#cancle").on("click", function(){
    
    window.location.href='findIdPw';
    
  });   // $("#cancel") click
  
  $("#changePw").on("click", function(){
    
      let registerPw = document.getElementById('rpassword2').value;
      let registerEmail =  $('#remail').val();
       
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

      if(!window.RoadScannerCredentialPolicy.isValidPassword(document.getElementById('rpassword').value)) {
          alert(window.RoadScannerCredentialPolicy.passwordMessage);
          document.getElementById('rpassword').focus();
          return false;
      }

      if(document.getElementById('rpassword').value !== document.getElementById('rpassword2').value) {
          alert("비밀번호가 일치하지 않습니다.");
          document.getElementById('rpassword2').focus();
          return false;
      }
      
      if(!emailVerified) {
		  alert("서버에서 이메일 인증을 완료해주세요.");
		  return false;
	  }
    
    $.ajax({
          type: "POST",
          url:"changePassword",
          dataType:"html",
          data:{
            password: $("#pw").val(),
            email: $("#email").val(),
			verificationToken: emailVerificationToken
          },
          success:function(data){
              let parsedJSON = JSON.parse(data);
              
              // 비밀번호 재설정 성공
              if("10" == parsedJSON.msgId) {
                alert(parsedJSON.msgContents);
                window.location.href="login";
              }
              
              // 비밀번호 재설정 실패
              if("20" == parsedJSON.msgId) {
                  alert(parsedJSON.msgContents);
				  resetPasswordEmailVerification();
                  return;
               }
              
            },
            error:function(){//실패시 처리
              console.error("Password reset request failed");
			  resetPasswordEmailVerification();
			  alert('비밀번호 재설정에 실패했습니다. 이메일 인증을 다시 진행해주세요.');
            }
        });  // ajax end
    
  });   // $("#changePw") click
  
  $("#emailDulpCheck").on("click",function(){
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
        url:"emailCheck",
        dataType:"html",
        data:{
          email: emial_str
        },
        success:function(data){
            let parsedJSON = JSON.parse(data);                        
            
            if("20" == parsedJSON.msgId){
              alert(parsedJSON.msgContents);  // javascript 메시지 다이얼 로그
              $("#remail").focus();
			  emailVerified = false;
			  emailVerificationToken = null;
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
          error:function(){//실패시 처리
            console.error("Email verification request failed");
          }
          
    }); //  $.ajax End --------------------------    
    
  });  // #idDulpCheck end
   
});   //모든 화면이 다 로딩이 되면 실행하는 영역
