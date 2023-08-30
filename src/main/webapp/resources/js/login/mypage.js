
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
            document.getElementById('rpassword2').value='';
            document.getElementById('rpassword2').focus();
        }
        
    }
    
}   // check_pw end

$(document).ready(function(){  //모든 화면이 다 로딩이 되면 실행하는 영역
   console.log("document ready");
   
   $("#myQnAboard").on("click", function(){
	   
	   window.location.href="/qna/my";
	   
   });
   
   $("#login").on("click", function(){
     
     window.location.href="/login";
     
   }); // login click
   
   $("#withdraw").on("click", function(){
        
      window.location.href="/withdraw";
      
    });   // $("#withdraw") click 

  $("#cancle").on("click", function(){

    	window.location.href="/main/preUpload";
    
  });   // $("#cancle") click
   
  $("#update").on("click", function(){
    if(""==$("#rpassword").val() || 0==$("#rpassword").val().length){
          alert("비밀번호를 입력하세요");  // javascript 메시지 다이얼 로그
          $("#rpassword").focus();
          return;
          
    } else {

		    $.ajax({
		          type: "POST",
		          url:"/update",
		          asyn:"true",
		          dataType:"html",
		          data:{
		            id: $("#rid").val(),
		            password: $("#rpassword").val(),
		            email: $("#remail").val()
		          },
		          success:function(data){
		              let parsedJSON = JSON.parse(data);
		              
		              // 업데이트 성공
		              if("10" == parsedJSON.msgId) {
		                alert(parsedJSON.msgContents);
		                window.location.href="/logout";
		              }
		              
		              // 업데이트  실패 (입력값 오류)
		              if("20" == parsedJSON.msgId) {
		                    alert(parsedJSON.msgContents);
		                    return;
		                }
		              
		              // 업데이트 실패 (현재 비밀번호와 동일한 값 입력)
		              if("30" == parsedJSON.msgId) {
		                    alert(parsedJSON.msgContents);
		                    return;
		                }           
		              
		            },
		            error:function(data){//실패시 처리
		              console.log("error:"+data);
		            }
		            
		        });  // ajax end
        
    }   // else end
    
  });   // $("#update") click
  
});   //모든 화면이 다 로딩이 되면 실행하는 영역