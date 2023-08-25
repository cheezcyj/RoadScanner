  <%@include file ="login/head.jsp" %>
  
<%@ page import="org.springframework.ui.Model"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib  prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
    // 서버 측에서 사용자 세션을 확인하고, 세션이 없으면 기본 페이지로 리다이렉트합니다.
    if (session.getAttribute("user") == null) {
      response.sendRedirect("/login");
    }
%>
<!DOCTYPE html>
<html>
<title>File Upload</title>

<header> <%@include file ="login/navbar.jsp" %> </header>
<body id="font-id">
<div id="separation">
<input id="thisName" type="hidden" value="${thisName}">
  <div class="left">
    <form action="fileUploaded" method="post" enctype="multipart/form-data" onsubmit="return false;">
      <div id="previewGroup" style="display: flex; flex-direction: row;">
        <label id="fileUploadLabel" for="fileUpload" style="display: none; cursor: pointer;">
          <img id="selectButtonImg" alt="SelectButton" src="${CP}/resources/img/selectButton.jpg" width="400" height="400">
        </label>
        <!-- 파일 선택 -->
        <input type="hidden" id="userid" value="${user.id}">
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
        <input class="btn btn-outline-secondary" type="submit" value="표지판 알아보기" id="runButton">
      </div>
    </form>
  </div>

  <div class="divider"></div>

  <div class="right" id="rightContent">
    <!-- 우측 영역의 내용을 입력 -->
    <h3 class="resultImgContent">${resultImg.content}</h3>
    <!-- 세로로 긴 내용 -->
    <div class="resultImgWrapper">
      <img id="resultImg" src="${resultImg.url}" alt="resultImg">
    </div>
    <div>${resultImg.name}</div>
    <p class="notice">의견을 전달해주시면, 이를 활용하여 보다 정확한 서비스를 제공하겠습니다.</p>
    <!-- 피드백 버튼 -->
    <div id="FeedbackButtons">
      <button id="likeButton" type="button" class="btn btn-link"><img src="${CP}/resources/img/thumbsup.jpg" alt="붐업 이미지"></button>
      <button id="dislikeButton" type="button" class="btn btn-link"><img src="${CP}/resources/img/thumbsdown.jpg" alt="붐따 이미지"></button>
    </div>
    <form id="reasonForm" method="post" style="display: none;">
      <div id="dislikeReason" class="card" style="border: none;">
        <div class="card-body">
          <c:forEach var="reason" items="${reasons}" varStatus="loop">
            <div class="form-check">
              <input class="form-check-input" type="checkbox" value="" id="reason${loop.count}" name="reason">
              <label class="form-check-label card-text ms-2" for="reason${loop.count}">
                ${reason}
              </label>
            </div>
          </c:forEach>
          <button class="btn btn-sm btn-secondary" id="submitButton" type="button" style="margin-top: 15px; margin-left: 70px;">선택</button>
        </div>
      </div>
    </form>
  </div>
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
  </script>
</body>
	<footer> <%@ include file="login/footer.jsp" %> </footer>
	<!-- CSS -->
	<link  href="${CP}/resources/css/upload.css" rel="stylesheet">
</html>