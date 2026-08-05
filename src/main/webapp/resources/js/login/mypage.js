
const myPageContextElement = document.querySelector('meta[name="application-context"]');
const myPageContextPath = myPageContextElement ? myPageContextElement.getAttribute('content') : '';
const myPageUrl = function (path) {
    return myPageContextPath + path;
};

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
            document.getElementById('rpassword2').value='';
            document.getElementById('rpassword2').focus();
        }
        
    }
    return pw === '' || document.getElementById('rpassword2').value === ''
        || pw === document.getElementById('rpassword2').value;
}   // check_pw end

$(document).ready(function(){  //모든 화면이 다 로딩이 되면 실행하는 영역
   $("#myQnAboard").on("click", function(){
	   
	   window.location.href = myPageUrl("/qna/my");
	   
   });
   
   $("#withdraw").on("click", function(){
        
      window.location.href = myPageUrl("/withdraw");
      
    });   // $("#withdraw") click 

  $("#cancle").on("click", function(){

    	window.location.href = myPageUrl("/main/preUpload");
    
  });   // $("#cancle") click
   
  $("#update").on("click", function(){
    if(""==$("#currentPassword").val() || 0==$("#currentPassword").val().length){
          alert("현재 비밀번호를 입력하세요");
          $("#currentPassword").focus();
          return;
    } else if(""==$("#rpassword").val() || 0==$("#rpassword").val().length){
          alert("비밀번호를 입력하세요");  // javascript 메시지 다이얼 로그
          $("#rpassword").focus();
          return;
    } else if(!window.RoadScannerCredentialPolicy.isValidPassword($("#rpassword").val())) {
          alert(window.RoadScannerCredentialPolicy.passwordMessage);
          $("#rpassword").focus();
          return;
    } else if($("#rpassword").val() !== $("#rpassword2").val()) {
          alert("비밀번호가 일치하지 않습니다.");
          $("#rpassword2").focus();
          return;
    } else {

		    $.ajax({
		          type: "POST",
		          url: myPageUrl("/update"),
		          async: true,
		          dataType: "json",
		          data:{
		            id: $("#rid").val(),
		            currentPassword: $("#currentPassword").val(),
		            password: $("#rpassword").val(),
		            email: $("#remail").val()
		          },
		          success:function(data){
		              // 업데이트 성공
		              if("10" == data.msgId) {
		                alert(data.msgContents);
		                window.location.href = myPageUrl("/login");
		              }
		              
		              // 업데이트  실패 (입력값 오류)
		              if("20" == data.msgId) {
		                    alert(data.msgContents);
		                    return;
		                }
		              
		              // 업데이트 실패 (현재 비밀번호와 동일한 값 입력)
		              if("30" == data.msgId) {
		                    alert(data.msgContents);
		                    return;
		                }           
		              
		            },
		            error:function(){//실패시 처리
		              console.error("Profile update request failed");
		            }
		            
		        });  // ajax end
        
    }   // else end
    
  });   // $("#update") click
  
});   //모든 화면이 다 로딩이 되면 실행하는 영역
