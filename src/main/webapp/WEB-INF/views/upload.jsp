<%@ page import="org.springframework.ui.Model"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib  prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="CP" value="${pageContext.request.contextPath }"/>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-rbsA2VBKQhggwzxH7pPCaAqO46MgnOM80zW1RWuH61DGLwZJEdK2Kadq2F9CUG65" crossorigin="anonymous">
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js" integrity="sha384-kenU1KFdBIe4zVF0s0G1M5b4hcpxyD9F7jL+jjXkk+Q2h455rYXK/7HAuoJl+0I4" crossorigin="anonymous"></script>
<script src="${CP}/resources/js/jquery-3.7.0.js"></script>
<title>File Upload</title>
<style>
  body {
    margin: 0;
    padding: 0;
    display: flex;
    height: 100vh;
    overflow: hidden;
  }
  
  .left {
    flex: 1;
    border: 1px solid #ccc;
    display: flex;
    justify-content: center;
    align-items: center;
  }
  
  .right {
    flex: 1;
    overflow: auto;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    text-align: center;
  }
  
  .divider {
    width: 1px;
    background-color: #ccc;
    height: 100%;
  }
  
  #fileUploadLabel {
    margin-left: 20px;
    display: inline-block;
    overflow: hidden;
    position: relative;
    height: 400px;
  }
  
  #fileUploadLabel img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    position: relative;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
  }
  
  #cancelContainer {
    position: relative;
  }
  
  #selectedImage {
    max-width: 500px;
  }
  
  #cancelButton {
    position: absolute;
    top: 5px;
    right: 5px;
    z-index: 2;
  }
  
  #cancelButton img{
    width: 30px;
    height: 30px;
  }
  
  #resultImg {
    width: 300px;
  }
  
  #FeedbackButtons {
    width: 45%;
    display: flex;
    justify-content: space-between;
    padding: 30px;
  }
  
  #FeedbackButtons img {
    width: 100px;
    height: 100px;
  }
  
  #dislikeReason {
    width: 220px;
    font-size: 16px;
    background-color: #f5f5f5;
    border: 1px solid #ccc;
    border-radius: 5px;
    text-align: center;
  }
  
  #reasonForm {
    margin-bottom: 30px;
    display: flex;
    justify-content: center;
    align-items: center;
    flex-wrap: wrap;
  }
  
  label {
    display: block;
    text-align: center;
  }
  
  input[type="submit"] {
    margin-top: 40px;
    display: block;
    width: 100%;
    height: 60px;
    font-size: 22px;
  }
  
  input[type="file"] {
    opacity: 0;
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    cursor: pointer;
    display: none;
  }
</style>
</head>
<body>
<input id="thisName" type="hidden" value="${thisName}">
  <div class="left">
    <form action="fileUploaded" method="post" enctype="multipart/form-data" onsubmit="return false;">
      <div id="previewGroup" style="display: flex; flex-direction: row;">
        <label id="fileUploadLabel" for="fileUpload" style="display: none; cursor: pointer;">
          <img id="selectButtonImg" alt="SelectButton" src="${CP}/resources/img/selectButton.jpg" width="400" height="400">
        </label>
        <!-- 파일 선택 -->
        <input id=fileUpload name="fileUpload" type="file" accept=".jpg, .jpeg, .png, .bmp, .tiff, .webp, .ico, .svg" onchange="displaySelectedFile(event)" style="display: none;">
        <div id="cancelContainer" style ="display: block;">
          <img id="selectedImage" src="${thisUrl}" alt="Selected Image">
          <button id="cancelButton" type="button" class="btn btn-link">
            <img alt="XButton" src="${CP}/resources/img/cancel.png">
          </button>
        </div>
      </div>
      <!-- 모델로 사진 전송, 실행 -->
      <div id="RunContainer" style="display: none;">
        <input type="submit" value="표지판 알아보기" id="runButton">
      </div>
    </form>
  </div>

  <div class="divider"></div>

  <div class="right" id="rightContent">
    <!-- 우측 영역의 내용을 입력 -->
    <h2>우측 영역</h2>
    <!-- 세로로 긴 내용 -->
    <label>
      <img id="resultImg" src="${resultImg}" alt="resultImg">
    </label>
    <p>[1]동해 물과 백두산이 마르고 닳도록 하느님이 보우하사 우리나라 만세 무궁화 삼천리 화려강산 대한 사람, 대한으로 길이 보전하세 [2]남산 위에 저 소나무, 철갑을 두른 듯 바람서리 불변함은 우리 기상일세 무궁화 삼천리 화려강산 대한 사람, 대한으로 길이 보전하세 [3]가을 하늘 공활한데 높고 구름 없이 밝은 달은 우리 가슴 일편단심일세 무궁화 삼천리 화려강산 대한 사람, 대한으로 길이 보전하세 [4]이 기상과 이 맘으로 충성을 다하여 괴로우나 즐거우나 나라 사랑하세 무궁화 삼천리 화려강산 대한 사람, 대한으로 길이 보전하세</p>
    <!-- 피드백 버튼 -->
    <div id="FeedbackButtons">
      <button id="likeButton" type="button" class="btn btn-link"><img src="${CP}/resources/img/thumbsup.jpg" alt="붐업 이미지"></button>
      <button id="dislikeButton" type="button" class="btn btn-link"><img src="${CP}/resources/img/thumbsdown.jpg" alt="붐따 이미지"></button>
    </div>
    <form id="reasonForm" method="post" style="display: none;">
      <div id="dislikeReason" class="card">
        <div class="card-body">
          <c:forEach var="reason" items="${reasons}" varStatus="loop">
            <div class="form-check">
              <input class="form-check-input" type="checkbox" value="" id="reason${loop.count}" name="reason">
              <label class="form-check-label card-text" for="reason${loop.count}">
                ${reason}
              </label>
            </div>
          </c:forEach>
          <button id="submitButton" type="button">선택</button>
        </div>
      </div>
    </form>
    <!-- 
    <form id="reasonForm" method="post" style="display: none;">
      <select id="dislikeReason" class="form-select form-select-lg mb-3" aria-label=".form-select-lg example" multiple size="3">
        <option value="" disabled selected hidden>무엇이 부족한가요?</option>
        <option value="reason1">업로드 오류</option>
        <option value="reason2">결과 오류</option>
        <option value="reason3">이해하기 어려움</option>
      </select>
      <button id="submitButton">선택</button>
    </form>
     -->
  </div>
  
  <script>
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
      
      let formData = new FormData();
        formData.append("fileUpload", $("#fileUpload")[0].files[0]);
        formData.append("idx", 1);
        formData.append("id", "testid");
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
                    "u2" : isSelected[1]
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
  </script>
</body>
</html>