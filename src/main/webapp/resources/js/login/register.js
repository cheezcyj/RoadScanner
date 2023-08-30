function check_pw() {
	var pw = document.getElementById('pw_form').value;
	var num = pw.search(/[0-9]/g);
	var eng = pw.search(/[a-z]/ig);
	var spe = pw.search(/[`~!@@#$%^&*|₩₩₩'₩";:₩/?]/gi);
	 
    if(pw.length<8 || pw.length>20) {
       window.alert('비밀번호는 8글자 이상, 20글자 이하만 이용 가능합니다.');
	   document.getElementById('pw_form').value='';
	   document.getElementById('pw_form').focus();
	} else if(pw.search(/\s/) != -1) {
	   window.alert('비밀번호는 공백 없이 이용 가능합니다');
	   document.getElementById('pw_form').value='';
	   document.getElementById('pw_form').focus();
	} else if(num < 0 || eng < 0 || spe < 0) {
	   window.alert('영문, 숫자, 특수문자를 최소 1글자 이상씩 사용하여 입력해주세요');
	   document.getElementById('pw_form').value='';
	   document.getElementById('pw_form').focus();
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
	  
	  
$(document).ready(function(){  //모든 화면이 다 로딩이 되면 실행하는 영역
	console.log("document ready");
	  
    $("#noneRegister").on("click", function(){
    	alert("회원가입을 취소했습니다");
    	window.location.href="/login";
	        
    });
	  
    $("#register").on("click", function(){
		console.log("register ready");
		
		let registerId = document.getElementById('id_form').value;
		let registerPw = document.getElementById('pw2_form').value;
		let registerEmail =  $('#email_front').val()+"@"+ $('#email_back').val()	  	   
	    
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
	      
		if("" == document.getElementById('email_front').value || "" == document.getElementById('email_back').value) {
			alert("이메일을 입력하세요");
		    return false;
		}
		
		if("" == document.getElementById('checkInput').value){
			alert("이메일 인증을 진행해주세요.");
		    return false;
		}
		
		if("1" == document.getElementById('auth').value) {
			alert("이메일 인증번호를 전송하지 않았습니다. 인증 후 가입을 진행해주세요");
			return false;
		}
		
		if( document.getElementById('checkInput').value != code){
			alert("이메일 인증번호가 다릅니다.");
		    return false;
		}
		
	          
		$.ajax({
		    type: "POST",
		    url:"/register",
		    dataType:"html",
		    data:{
		    	grade: $("#grade").val(),
		    	id: $("#id").val(),
		    	password: $("#pw").val(),
		    	email: $("#email").val()
		    },
		    success:function(data){
			    let parsedJSON = JSON.parse(data);
			    console.log("parsedJSON.msgId:"+parsedJSON.msgId);
			                     
			    
			    if("10" == parsedJSON.msgId){
			      alert(parsedJSON.msgContents);
			      window.location.href="/login";
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
	

	$("#idDulpCheck").on("click",function(){
		console.log("idDulpCheck ready");
		
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
	        url:"/idDulpCheck",
	        dataType:"html",
	        data:{
	        	id: $("#id_form").val()
	        },
	        
			success:function(data){
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
	      url:"/emailDulpCheck",
	      dataType:"html",
	      data:{
	    	  email: emial_str
	      },
		  success:function(data){
			  let parsedJSON = JSON.parse(data);
		                   
			  if("10" == parsedJSON.msgId){
			    alert(parsedJSON.msgContents);
			    $("#email_front").focus();
			  } 
			  
			  if("20" == parsedJSON.msgId){
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
	 
}); // document end
	
$('#mail-Check-Btn').click(function() {
	const email = $('#email_front').val()+"@"+ $('#email_back').val()
	const checkInput = $('#checkInput'); // 인증번호 입력하는곳
	
	if ("" == document.getElementById('email_front').value || "" == document.getElementById('email_back').value) {
		alert('이메일을 입력하십시오.');
		email_Check = false;
		$("#email").focus();
		
	} else if("ok" != document.getElementById('emailok').value) {
		alert('이메일을 중복확인후에 진행하시오.');
		
	} else {
		
	    $.ajax({
	        type : 'get',
	        url : "mailCheck?email=" + email,
	        success : function(data) {

		    $('#checkInput').attr('disabled', false);
		    code = data;
		    $('#auth').attr('value', 2);
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
        $resultMsg.html('인증번호 일치');
        $resultMsg.css('color', 'green');
        $resultMsg.css('display', 'block');
        $resultMsg.css('font-size', '13px');
        $('#mail-Check-Btn').attr('disabled', true);
        $('#email').attr('readonly', true);
        $('#register').attr('disabled', false);
        certified_Email = true;
        
    } else {
        certified_Email = false;
        $resultMsg.html('인증번호 불일치');
        $resultMsg.css('color', 'red');
        $resultMsg.css('display', 'block');
        $resultMsg.css('font-size', '13px');
        
    }
    
});