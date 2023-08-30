    $("#doLogin").on("click",function(){
      
      
      if(confirm('로그인 하시겠습니까?')==false) return;
        
      if(""==$("#id").val() || 0==$("#id").val().length){
          alert("아이디를 입력하세요");  // javascript 메시지 다이얼 로그
          $("#id").focus();          // jquery로 포커스를 이동시킨다.
          return;
      }
      if(""==$("#pd").val() || 0==$("#pw").val().length){
        alert("비밀번호를 입력하세요");  // javascript 메시지 다이얼 로그
        $("#pw").focus();
        return;
      }
      
      $.ajax({
            type: "POST",
            url:"/login",
            dataType:"html",
            data:{
              id: $("#id").val(),
              password: $("#pw").val()
            },
            success:function(data){//통신 성공
                //console.log("success data:"+data);
                // JSON.parse() 메서드는 JSON 문자열의 구문을 분석하고, 그 결과에서 JavaScript 값이나 객체를 생성합니다.
                let paredJSON = JSON.parse(data);
            
                
                if("1"==paredJSON.msgId || "10"==paredJSON.msgId){
                  alert(paredJSON.msgContents);  // javascript 메시지 다이얼 로그
                  $("#id").focus();          // jquery로 포커스를 이동시킨다.
                  return;
                }
                if("2"==paredJSON.msgId || "20"==paredJSON.msgId){
                  alert(paredJSON.msgContents);
                  $("#pw").focus();
                  return;
                }
                if("30"==paredJSON.msgId){//로그인 성공
                  window.location.href="/main/preUpload";
                }
                if("40"==paredJSON.msgId) {
                	alert(paredJSON.msgContents);
                	document.getElementById('id').value='';
                	document.getElementById('pw').value='';
                }
              },
              error:function(data){//실패시 처리
                console.log("error:"+data);
              }
          });
    });    