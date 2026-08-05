    const dislikeButton = document.getElementById('dislikeButton');
    const reasonForm = document.getElementById('reasonForm');
    const contextPath = $("#contextPath").val();

    function keepSubmittedResultVisible() {
      $("#likeButton, #dislikeButton, #submitButton").prop("disabled", true);
      reasonForm.style.display = 'none';
      dislikeButton.setAttribute('aria-expanded', 'false');
    }

    if ($("#feedbackSubmitted").val() === "true") {
      keepSubmittedResultVisible();
    }

    // 선택 상자--------------------------------------------------------------------
    // likeButton 클릭 시 category 20으로 update
    $("#likeButton").on("click", function(){
      if (confirm("제출하시겠습니까?")) {
        $.ajax({
            type: "POST",
            url:contextPath + "/main/feedbackUpdate",
            async: true,
            data:{
                  "idx" : $("#thisIdx").val(),
                  "category" : 20,
                  "checked" : 0,
                  "u1" : 0,
                  "u2" : 0,
                  "u3" : 0
            },
            success:function(data){ //통신 성공
              if("1" == data.msgId){
                  alert('소중한 의견 감사드립니다.');
                  keepSubmittedResultVisible();
              }else{
                  alert(data.msgContents);
                  alert("오류 발생. 다시 시도해 주세요.");
              }
            },
            error:function(data){   //실패시 처리
               console.error("Feedback update failed");
            }
        }); // ajax End
          
      } else {
        return;
      }// if End
    }); // likeButton End
    
    // dislikeButton 클릭 시 선택 상자 토글
    $("#dislikeButton").on("click", function(){
        const shouldOpen = reasonForm.style.display === 'none';
        reasonForm.style.display = shouldOpen ? 'flex' : 'none';
        dislikeButton.setAttribute('aria-expanded', String(shouldOpen));
    });
    
    // 선택상자의 submitButton 클릭 시 category 30으로, 싫어요 이유 update
    $("#submitButton").on("click", function(){
        let isSelected = [];

        $("input[name^='reason']").each(function() {
            if (this.checked) {
                isSelected.push(1);
            } else {
                isSelected.push(0);
            }
        });

        if (isSelected.includes(1) !== true) {
            alert("하나 이상의 이유를 선택하세요.");
            return;
        }

        if (confirm("제출하시겠습니까?")) {
            $.ajax({
                type: "POST",
                url:contextPath + "/main/feedbackUpdate",
                async: true,
                data:{
                    "idx" : $("#thisIdx").val(),
                    "category" : 30,
                    "checked" : 0,
                    "u1" : isSelected[0],
                    "u2" : isSelected[1],
                    "u3" : isSelected[2]
                },
                success:function(data){
                    if("1" == data.msgId){
                        alert('소중한 의견 감사드립니다.');
                        keepSubmittedResultVisible();
                    }else{
                        alert(data.msgContents);
                        alert("오류 발생. 다시 시도해 주세요.");
                    }
                },
                error:function(data){
                    console.error("Feedback update failed");
                }
            });
        }
    });
  
    // 선택 상자 End-----------------------------------------------------------------
