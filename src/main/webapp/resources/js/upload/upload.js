    // 선택한 이미지 미리보기로 보여주기-------------------------------------------------------
    // 이미지 미리보기 취소
    const fileUploadInput = document.getElementById('fileUpload');
    const fileUploadLabel = document.getElementById('fileUploadLabel');
    const selectButtonImg = document.getElementById('selectButtonImg');
    const cancelContainer = document.getElementById('cancelContainer');
    const selectedImage = document.getElementById('selectedImage');
    const cancelButton = document.getElementById('cancelButton');
    const RunContainer = document.getElementById('RunContainer');
    
    function displaySelectedFile(event) {
      const file = event.target.files[0];
      if (file) {
        // 파일 크기 체크 (5MB)
        const maxSize = 5 * 1024 * 1024;
        if (file.size > maxSize) {
            alert('최대 5MB인 이미지만 선택 가능합니다.');
            fileUploadInput.value = '';
            return;
        }
        
        const reader = new FileReader();

        reader.onload = function() {
          fileUploadLabel.style.display = 'none';        // 파일선택 버튼 이미지 숨기기
          cancelContainer.style.display = 'block';       // 미리보기 보이기
          selectedImage.src = reader.result;
          RunContainer.style.display = 'block'; // "표지판 알아보기" 버튼 보이기
          
        };
        
        // 추가: 허용된 이미지 확장자 체크
        const allowedExtensions = ['jpg', 'jpeg', 'png', 'bmp', 'tiff', 'webp', 'ico', 'svg'];
        const fileExtension = file.name.split('.').pop().toLowerCase();        
        
        if (!allowedExtensions.includes(fileExtension)) {
          alert('이미지 파일이 아닙니다.');
          fileUploadInput.value = '';
          return;
        }
        
        reader.readAsDataURL(file);
        //console.log(`displaySelectedFile`);
      } else {
        fileUploadLabel.style.display = 'block';      // 파일선택 버튼 이미지 보이기
        cancelContainer.style.display = 'none';       // 미리보기 숨기기
        RunContainer.style.display = 'none'; // "표지판 알아보기" 버튼 숨기기
      }
    }
    
    // 취소 버튼 클릭 이벤트 리스너 추가
    cancelButton.addEventListener('click', function() {
      fileUploadInput.value = '';                   // 파일 선택 취소
      fileUploadLabel.style.display = 'block';      // 파일선택 버튼 이미지 보이기
      cancelContainer.style.display = 'none';       // 미리보기 숨기기
      RunContainer.style.display = 'none'; // "표지판 알아보기" 버튼 숨기기
      //console.log(`displaySelectedFile: None`);
    });
    // 선택한 이미지 미리보기로 보여주기 End----------------------------------------------------

    // 파일 업로드-------------------------------------------------------------------
    //화면 오른쪽에 결과창 띄우는 함수
    function showRightContent() {
      let rightDiv = document.getElementById('rightContent');
      rightDiv.style.display = 'block';
    }
    
    // '표지판 알아보기'클릭 시 파일 업로드, 결과창 나타내기
    $("#runButton").on("click", function(){
      //console.log('runButton click');

      let userid = $("#userid").val();
      let formData = new FormData();
        formData.append("fileUpload", $("#fileUpload")[0].files[0]);
        formData.append("idx", 1);
        formData.append("id", userid);
        formData.append("category", 10);
        formData.append("name", "testname");
        formData.append("url", "testurl");
        formData.append("fileSize", 0);
        formData.append("checked", 0);
        formData.append("u1", 0);
        formData.append("u2", 0);
      
      $.ajax({
          type: "POST",
          url: "/main/fileUploaded",
          processData: false,
          contentType: false,
          data: formData,
          success: function(data) { // 통신 성공
              console.log(data);
              // 여기서 페이지 이동
              window.location.href = "/main/upload?imgName=" + encodeURIComponent(data);
          },
          error: function(data) { // 실패시 처리
              console.error("파일 업로드 오류:", data.msgId, data.msgContents);
          }
      });
      
    });
    // 파일 업로드 End----------------------------------------------------------------
    
    // 선택 상자--------------------------------------------------------------------
    const dislikeButton = document.getElementById('dislikeButton');
    const dislikeReason = document.getElementById('dislikeReason');
    const reasonForm = document.getElementById('reasonForm');
    const likeButton = document.getElementById('likeButton');
    const submitButton = document.getElementById('submitButton');
    
    // likeButton 클릭 시 category 20으로 update
    $("#likeButton").on("click", function(){
      if (confirm("제출하시겠습니까?")) {

        $.ajax({
            type: "POST",
            url:"/main/feedbackUpdate",
            asyn:"true",
            data:{
                  "name" : $("#thisName").val(),
                  "category" : 20,
                  "checked" : 0,
                  "u1" : 0,
                  "u2" : 0
            },
            success:function(data){ //통신 성공
              console.log("feedback update:", data);
              if("1" == data.msgId){
                  alert('소중한 의견 감사드립니다.');
                  window.location.href = "/main/preUpload";
              }else{
                  alert(data.msgContents);
                  alert("오류 발생. 다시 시도해 주세요.");
              }
            },
            error:function(data){   //실패시 처리
               console.error("feedback update error:", data);
            }
        }); // ajax End
          
      } else {
        return;
      }// if End
    }); // likeButton End
    
    // dislikeButton 클릭 시 선택 상자 토글
    $("#dislikeButton").on("click", function(){
        reasonForm.style.display = reasonForm.style.display === 'none' ? 'block' : 'none';
        dislikeReason.style.display = reasonForm.style.display;
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
                url:"/main/feedbackUpdate",
                asyn:"true",
                data:{
                    "name" : $("#thisName").val(),
                    "category" : 30,
                    "checked" : 0,
                    "u1" : isSelected[0],
                    "u2" : isSelected[1],
                    "u3" : isSelected[2]
                },
                success:function(data){
                    console.log("feedback update:", data);
                    if("1" == data.msgId){
                        alert('소중한 의견 감사드립니다.');
                        window.location.href = "/main/preUpload";
                    }else{
                        alert(data.msgContents);
                        alert("오류 발생. 다시 시도해 주세요.");
                    }
                },
                error:function(data){
                    console.error("feedback update error:", data);
                }
            });
        }
    });
  
    // 선택 상자 End-----------------------------------------------------------------
        // 폼 제출하고 새로고침
        //event.preventDefault();
        //reasonForm.submit();
        //alert('소중한 의견 감사드립니다.');
        //window.location.reload();