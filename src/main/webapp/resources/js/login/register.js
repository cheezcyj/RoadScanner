let emailVerified = false;
let emailVerificationToken = null;

function resetRegistrationEmailVerification() {
	emailVerified = false;
	emailVerificationToken = null;
	$('#mail-Check-Btn').attr('disabled', false);
	$('#email_front, #email_back').attr('readonly', false);
	$('#checkInput').attr('disabled', false).val('');
}

function check_pw() {
	var passwordInput = document.getElementById('pw_form');
	var pw = passwordInput.value;
	if (pw && !window.RoadScannerCredentialPolicy.isValidPassword(pw)) {
		window.alert(window.RoadScannerCredentialPolicy.passwordMessage);
		passwordInput.value = '';
		passwordInput.focus();
		return false;
	}
	
	if(document.getElementById('pw_form').value !='' && document.getElementById('pw2_form').value!='') {
	  
	    if(document.getElementById('pw_form').value == document.getElementById('pw2_form').value) {
	    document.getElementById('pw_check').innerHTML='비밀번호가 일치합니다.'
	    document.getElementById('pw_check').style.color='blue';
	    document.getElementById('pw_check').style.fontSize='15px';
	    
	} else {
	    document.getElementById('pw_check').innerHTML='비밀번호가 일치하지 않습니다.';
	    document.getElementById('pw_check').style.color='red';
	    document.getElementById('pw_check').style.fontSize='15px';
	    document.getElementById('pw2_form').value='';
	    document.getElementById('pw2_form').focus();
		
		}
	        
		return true;
	}
	return pw === '';
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
	  
	  
$(document).ready(function(){  //모든 화면이 다 로딩이 되면 실행하는 영역
	  
    $("#noneRegister").on("click", function(){
    	alert("회원가입을 취소했습니다");
    	window.location.href="login";
	        
    });
	  
    $("#register").on("click", function(){
		let registerId = document.getElementById('id_form').value;
		let registerPw = document.getElementById('pw2_form').value;
		let registerEmail = $('#email_front').val()+"@"+ $('#email_back').val();
	    
	    document.register_form.id.value = registerId;
	    document.register_form.pw.value = registerPw;
	    document.register_form.email.value = registerEmail;
	    	    
	 
	    if("" == document.getElementById('id_form').value) {
			alert("아이디를 입력하세요");
		    return false;
		}
	
		if("ok" != document.getElementById('idok').value) {
			alert("아이디 인증을 진행하세요.");
		    return false;
		}
	
		if("" == document.getElementById('pw_form').value || "" == document.getElementById('pw2_form').value) {
			alert("비밀번호를 입력하세요");
		    return false;
		}

		if(!window.RoadScannerCredentialPolicy.isValidPassword(document.getElementById('pw_form').value)) {
			alert(window.RoadScannerCredentialPolicy.passwordMessage);
			document.getElementById('pw_form').focus();
			return false;
		}

		if(document.getElementById('pw_form').value !== document.getElementById('pw2_form').value) {
			alert("비밀번호가 일치하지 않습니다.");
			document.getElementById('pw2_form').focus();
			return false;
		}
	      
		if("" == document.getElementById('email_front').value || "" == document.getElementById('email_back').value) {
			alert("이메일을 입력하세요");
		    return false;
		}
		
		if("" == document.getElementById('checkInput').value){
			alert("이메일 인증을 진행해주세요.");
		    return false;
		}
		
		if(!emailVerified) {
			alert("서버에서 이메일 인증을 완료해주세요.");
		    return false;
		}
		
	          
		$.ajax({
		    type: "POST",
		    url:"register",
		    dataType:"html",
		    data:{
		    	grade: $("#grade").val(),
		    	id: $("#id").val(),
		    	password: $("#pw").val(),
		    	email: $("#email").val(),
				verificationToken: emailVerificationToken
		    },
		    success:function(data){
			    let parsedJSON = JSON.parse(data);
			                     
			    
			    if("10" == parsedJSON.msgId){
			      alert(parsedJSON.msgContents);
			      window.location.href="login";
			    } 
			    
			  if("20" == parsedJSON.msgId){
			    alert(parsedJSON.msgContents);
			    resetRegistrationEmailVerification();
			    return;
			  }
			    
			   
			  },
			  
			  error:function(){//실패시 처리
			    console.error("Registration request failed");
				resetRegistrationEmailVerification();
				alert('회원가입 처리에 실패했습니다. 이메일 인증을 다시 진행해주세요.');
			  }
			  
		}); //  $.ajax End --------------------------
	    	    
});    // #register END
	

	$("#idDulpCheck").on("click",function(){
		var id_str = document.getElementById('id_form').value;
	   
	    
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
	        url:"idDulpCheck",
	        dataType:"html",
	        data:{
	        	id: $("#id_form").val()
	        },
	        
			success:function(data){
			    let parsedJSON = JSON.parse(data);
			                     			    
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
			  error:function(){//실패시 처리
			    console.error("User ID availability request failed");
			}
			
	    }); //  $.ajax End --------------------------	    
	    
	});  // #idDulpCheck end
	
$("#emailDulpCheck").on("click",function(){
	var emial_str = $('#email_front').val()+"@"+ $('#email_back').val();
	
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
	  document.getElementById('email_back').value='';
      return;
	}
	  
	  $.ajax({
	      type: "POST",
	      url:"emailDulpCheck",
	      dataType:"html",
	      data:{
	    	  email: emial_str
	      },
		  success:function(data){
			  let parsedJSON = JSON.parse(data);
		                   
			  if("10" == parsedJSON.msgId){
			    alert(parsedJSON.msgContents);
			    $("#email_front").focus();
			    emailVerified = false;
			  } 
			  
			  if("20" == parsedJSON.msgId){
			    alert(parsedJSON.msgContents);
			    $('#emailok').attr('value',"ok");
			    return;
			  }	  
	 
		  },
		  error:function(){//실패시 처리
			  console.error("Email availability request failed");
		  }
		  
	  	}); //  $.ajax End --------------------------
    	    
	});  // #idDulpCheck end
	 
}); // document end
	
$('#mail-Check-Btn').click(function() {
	const email = $('#email_front').val()+"@"+ $('#email_back').val();
	
	if ("" == document.getElementById('email_front').value || "" == document.getElementById('email_back').value) {
		alert('이메일을 입력하십시오.');
		emailVerified = false;
		$("#email").focus();
		
	} else if("ok" != document.getElementById('emailok').value) {
		alert('이메일을 중복확인후에 진행하시오.');
		
	} else {
		
	    $.ajax({
	        type : 'POST',
	        url : "mailCheck",
	        dataType : "json",
	        data : { email: email },
	        success : function(data) {
			if ("10" !== data.msgId) {
				alert(data.msgContents);
				return;
			}

		    $('#checkInput').attr('disabled', false).val('').focus();
		    $('#auth').attr('value', 2);
			emailVerified = false;
			emailVerificationToken = null;
		    alert(data.msgContents);
	        },
			error : function() {
				alert('인증번호 전송에 실패했습니다. 잠시 후 다시 시도해주세요.');
			}
	    
	    }); // end ajax
	    
	}
	
}); // end send eamil

// 인증번호 비교
// blur -> focus가 벗어나는 경우 발생
$('#checkInput').blur(function() {
    const inputCode = $(this).val();
    const $resultMsg = $('#mail-check-warn');

	if (!/^\d{6}$/.test(inputCode)) {
		emailVerified = false;
		$resultMsg.html('인증번호 6자리를 입력해주세요.').css({color: 'red', display: 'block', fontSize: '13px'});
		return;
	}

	const email = $('#email_front').val()+"@"+ $('#email_back').val();
	$.ajax({
		type: 'POST',
		url: 'mailCheck/verify',
		dataType: 'json',
		data: { email: email, code: inputCode },
		success: function(data, textStatus, jqXHR) {
			const receivedToken = jqXHR.getResponseHeader('X-Email-Verification-Token');
			emailVerified = data.msgId === '10' && !!receivedToken;
			emailVerificationToken = emailVerified ? receivedToken : null;
			$resultMsg.html(data.msgContents).css({
				color: emailVerified ? 'green' : 'red',
				display: 'block',
				fontSize: '13px'
			});
			if (emailVerified) {
				$('#mail-Check-Btn').attr('disabled', true);
				$('#email_front, #email_back').attr('readonly', true);
				$('#checkInput').attr('disabled', true);
				$('#register').attr('disabled', false);
			}
		},
		error: function() {
			emailVerified = false;
			emailVerificationToken = null;
			$resultMsg.html('인증 확인에 실패했습니다. 다시 시도해주세요.').css('color', 'red');
		}
	});
});

$('#email_front, #email_back').on('input', function() {
	emailVerified = false;
	emailVerificationToken = null;
	$('#emailok').val('');
	$('#auth').val(1);
	$('#mail-check-warn').empty();
});
