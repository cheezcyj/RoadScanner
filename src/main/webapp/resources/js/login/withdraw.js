   $(document).ready(function() {

       $("#withdraw").click(function() {
           var password = $("#upassword").val();

           // 확인 메시지 표시
           if (!confirm('회원 탈퇴하시겠습니까?') == true) {
               return false;
           }
      
           else {

	           // AJAX 요청을 보냅니다.
	           $.ajax({
	               type: "POST",
	               url:"/withdraw",
	               dataType:"html",
	               data: {
	                id: $("#id").val(),
	                password: $("#rawPassword").val()
	               },
	               success:function(data) {
	                let parsedJSON = JSON.parse(data);
	                  
	                    if("10" == parsedJSON.msgId){
	                          alert(parsedJSON.msgContents);
	                          window.location.href="/logout";
	                   } 
	                                         
	                   if("20" == parsedJSON.msgId){
	                       alert(parsedJSON.msgContents);
	                       return;
	                   }
	                   
	               },
	               error: function(data) {
	                   console.log("error:" + data);
	               }
	           }); // --ajax
           
           } // -- else
        	   
       }); // --doWithdraw
       
   });