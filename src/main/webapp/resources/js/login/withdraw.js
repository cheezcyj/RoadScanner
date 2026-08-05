   const withdrawalContextElement = document.querySelector('meta[name="application-context"]');
   const withdrawalContextPath = withdrawalContextElement
       ? withdrawalContextElement.getAttribute('content')
       : '';

   $(document).ready(function() {

       $("#withdraw").click(function() {
           // 확인 메시지 표시
           if (!confirm('회원 탈퇴하시겠습니까?')) {
               return false;
           }
      
           else {

	           // AJAX 요청을 보냅니다.
	           $.ajax({
	               type: "POST",
	               url: withdrawalContextPath + "/withdraw",
	               dataType: "json",
	               data: {
	                id: $("#id").val(),
	                password: $("#rawPassword").val()
	               },
	               success:function(data) {
	                    if("10" == data.msgId){
	                          alert(data.msgContents);
	                          window.roadscannerLogout();
	                   } 
	                                         
	                   if("20" == data.msgId){
	                       alert(data.msgContents);
	                       return;
	                   }
	                   
	               },
	               error: function() {
	                   console.error("Account withdrawal request failed");
	               }
	           }); // --ajax
           
           } // -- else
        	   
       }); // --doWithdraw
       
   });
