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
          fileUploadLabel.style.display = 'none';    // 파일선택 버튼 이미지 숨기기
          cancelContainer.style.display = 'block';   // 미리보기 보이기
          selectedImage.src = reader.result;
          RunContainer.style.display = 'block';      // "표지판 알아보기" 버튼 보이기
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
        fileUploadLabel.style.display = 'block';    // 파일선택 버튼 이미지 보이기
        cancelContainer.style.display = 'none';     // 미리보기 숨기기
        RunContainer.style.display = 'none';        // "표지판 알아보기" 버튼 숨기기
      }
    }
    
    // 취소 버튼 클릭 이벤트 리스너 추가
    cancelButton.addEventListener('click', function() {
      fileUploadInput.value = '';                   // 파일 선택 취소
      fileUploadLabel.style.display = 'block';      // 파일선택 버튼 이미지 보이기
      cancelContainer.style.display = 'none';       // 미리보기 숨기기
      RunContainer.style.display = 'none';          // "표지판 알아보기" 버튼 숨기기
      //console.log(`displaySelectedFile: None`);
    });
    // 선택한 이미지 미리보기로 보여주기 End----------------------------------------------------

    // 파일 업로드-------------------------------------------------------------------
    // '표지판 알아보기'클릭 시 파일 업로드, /upload로 넘어가기
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
