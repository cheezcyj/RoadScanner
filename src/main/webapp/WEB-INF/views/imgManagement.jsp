<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib  prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="CP" value="${pageContext.request.contextPath }"/>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-9ndCyUaIbzAi2FUVXJi0CjmCapSmO7SnpJef0486qhLnuZ2cdeRhO02iuK6FUUVM" crossorigin="anonymous">
<script src="${CP}/resources/js/jquery-3.7.0.js"></script>
<title>imgManagement</title>
<style>
	.image-container {
    position: relative;
    display: inline-block;
	}
	.image-container img {
	  width:300px;
	  object-fit: cover;
    cursor: pointer;
	}
  .checkbox-local {
    position: absolute;
    top: 5px;
    left: 5px;
    z-index: 2;
    cursor: pointer;
  } 
  /* 체크박스 선택 시 액션 CSS */
  input.btn_check{
    display:none; /* 체크하기 전 버튼 숨김 */
  }
  input.btn_check + label{
    cursor:pointer; /* 커서 */
  }
  input.btn_check + label:before{ /* 체크하기 전 label CSS */
    content:"";
    font-size: 7px;
    text-align: center;
    display:inline-block;
    width:20px;
    height:20px;
    border:2px solid #212529;
    border-radius: 4px;
    position: absolute;
    left: 0;
    top:0;
  }
  input.btn_check:checked + label:before{ /* 체크 후 label CSS */
    content:"";
    background-color:#212529;
    border-color:#212529;
    background-image: url('${CP}/resources/img/checkmark.png');
    background-repeat: no-repeat;
    background-position: 50%;
    background-size: 16px 16px;
  }
    
  .delete-save-button {
    position: absolute;
    top: -75px;
    left: -5px;
    gap: 15px;
    padding: 10px;
    cursor: pointer;
    z-index: 3;
  }
  .select-all-button {
    position: absolute;
    top: -75px;
    left: 82%;
    padding: 10px;
    cursor: pointer;
    z-index: 3;
  }
  .modal-delete-save-button {
    gap: 15px;
    padding-bottom: 20px;
    cursor: pointer;
  }
  
  /* 이미지 크게 보기 창(modal) CSS */
  .image-modal {
    position: fixed;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    background-color: white;
    z-index: 999;
    width: 1301px;
    height: 800px;
    max-width: 100%;
    max-height: 100%;
    display: none;
  }
  .image-modal-close {
    position: absolute;
    top: 3px;
    right: 3px;
    cursor: pointer;
  }
  .left {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 900px;
    height: 800px;
  }
  .right {
    overflow: auto;
    align-items: center;
    width: 400px;
    padding: 30px;
    height: 800px;
  }
  .divider {
    width: 1px;
    background-color: #ccc;
    height: 100%;
  }
  .sort-horizon {
    display: flex;
  }
</style>
</head>
<body>
  <div class="container">
    <div class="">
      <button type="button" id="selectDeleteBtn" class="btn btn-outline-danger" onclick="selectDelete()">DELETE</button>
      <button type="button" id="selectSaveBtn" class="btn btn-dark" style="width: 200px;" onclick="selectSave()">SAVE</button>
    </div>
    
    <div>
	    <select id="categoryDropdown" name="category">
	      <option selected="selected" disabled="disabled">분류</option>
	      <option value="0">전체</option>
	      <option value="10">기본</option>
	      <option value="20">좋아요</option>
	      <option value="30">싫어요</option>
	    </select>
	    <input type="checkbox" id="selectAllBtn" onclick="toggleSelectAll()" style="display: none;">
	    <label for="selectAllBtn" class="btn btn-link" style="color: #212529;">SELECT_ALL</label>
    </div>
    
    
    <%-- ${list} --%>
    <!-- 3*3 사진+체크박스 디스플레이 -->
    <div class="d-flex justify-content-center">
			<table id="photoList">
			  <c:choose>
			    <c:when test="${not empty list}">
			      <c:forEach var="vo" items="${list}" varStatus="status">
			        <c:if test="${status.index % 3 == 0}">
			          <tr>
			        </c:if>
				        <td>
				          <div class="image-container">
				            <div class="checkbox-local">
				              <input type="checkbox" class="btn_check" id="${vo.name}">
				              <label for="${vo.name}"></label>
				            </div>
				            <div>
					            <img class="uploaded-image" src="${vo.url}" alt="${vo.name}">
				            </div>
				          </div> <!-- image-container end -->
				        </td>
			        <c:if test="${(status.index + 1) % 3 == 0 || status.last}">
			          </tr>
			        </c:if>
			      </c:forEach>
			    </c:when>
			    <c:otherwise>
			      <td rowspan="3" colspan="3">No data found</td>
			    </c:otherwise>
			  </c:choose>
			</table>
		</div>
	  
	  <!-- 페이징 -->
		  <ul class="pagination justify-content-center">
		    <li class="page-item">
		      <a class="page-link" href="#" aria-label="Previous">
		        <span aria-hidden="true">&laquo;</span>
		      </a>
		    </li>
		    <li class="page-item"><a class="page-link" href="#">1</a></li>
		    <li class="page-item"><a class="page-link" href="#">2</a></li>
		    <li class="page-item"><a class="page-link" href="#">3</a></li>
		    <li class="page-item">
		      <a class="page-link" href="#" aria-label="Next">
		        <span aria-hidden="true">&raquo;</span>
		      </a>
		    </li>
		  </ul> <!-- 페이징 -->
	  
  </div> <!-- container -->
  
  <!----------------------------- 이미지 크게 보기 창 ----------------------------->
  <div class="image-modal" id="imageModal">
    <div class="sort-horizon">
      <div class="left">
        <img src="" id="modalImage" style="max-width: 700px; max-height: 800px;">
        <span class="image-modal-close">&times;</span>
      </div>
      <div class="divider"></div>
      <div class="right">
        <div style="height: 650px;">
          <table class="table">
            <tr>
              <td>번호</td>
              <td id="idx"></td>
            </tr>
            <tr>
              <td>이름</td>
              <td id="name"></td>
            </tr>
            <tr>
              <td>업로더</td>
              <td id="id"></td>
            </tr>
            <tr>
              <td>날짜</td>
              <td id="uploadDate"></td>
            </tr>
            <tr>
              <td>크기</td>
              <td id="fileSize"></td>
            </tr>
             <tr>
              <td>상태</td>
              <td id="category"></td>
            </tr>
            <tr>
              <td>오류1</td>
              <td id="u1"></td>
            </tr>
            <tr>
              <td>오류2</td>
              <td id="u2"></td>
            </tr>
          </table>
        </div>
        <div class="modal-delete-save-button">
          <button type="button" id="detailDeleteBtn" class="btn btn-outline-danger" style="margin-left: 4px; height: 45px;">DELETE</button>
          <button type="button" id="detailSaveBtn" class="btn btn-dark" style="width: 250px; height: 45px;">SAVE</button>
        </div>
      </div>
    </div>
  </div>
  
<script>
	
  // SELECT_ALL 버튼 눌러 전체 선택
  function toggleSelectAll() {
    let checkboxes = document.querySelectorAll(".btn_check");
    let selectAllBtn = document.getElementById("selectAllBtn");
    let checked = selectAllBtn.checked;
    
    for (let i = 0; i < checkboxes.length; i++) {
      checkboxes[i].checked = checked;
    }
  } // toggleSelectAll()
  

  //선택 삭제
  function selectDelete() {  
	  
	  // 선택된 체크박스의 idx 추출
	  let checkboxes = [];
    $(".btn_check:checked").each(function() {
      let name = $(this).attr("id");
      checkboxes.push(name);
    });
    
    console.log("checkboxes: " + checkboxes)
    
	  if (checkboxes.length === 0) {    
        alert("선택된 이미지가 없습니다.");
        return;
    }
    
    if (confirm("삭제하시겠습니까?")) {
    	
    	$.ajax({
    		  type: "POST",
    		  url:"/doDeleteMultiple",
    		  asyn:"true",
    		  traditional: true, // 배열 데이터 전송을 위해 traditional 옵션을 설정
    	    data: { checkboxes: checkboxes },
    		  success:function(data){//통신 성공
    			  console.log("data: " + data)
   	          if("1" == data) {
   	            alert("삭제되었습니다.");
   	            location.reload();
   	          } else {
   	            alert("삭제를 실패했습니다.");
   	          }
    		  },
    		  error:function(data){//실패시 처리
    		    console.log("error:"+data);
    		  }
 		  }); // ajax
    } // if
  } // selectDelete()
  
  
  //선택 저장
  function selectSave() {
	    
	  // 선택된 체크박스의 idx 추출
	  let checkboxes = [];
	  $(".btn_check:checked").each(function() {
	    let name = $(this).attr("id")
	    checkboxes.push(name);
	  });
	  
	  console.log("checkboxes: " + checkboxes)
	  
	  if (checkboxes.length === 0) {    
	      alert("선택된 이미지가 없습니다.");
	      return;
	  }
    
    if (confirm("저장하시겠습니까?")) {
	    $.ajax({
	        type: "POST",
	        url:"/checkedUpdateMultiple",
	        asyn:"true",
	        traditional: true, // 배열 데이터 전송을 위해 traditional 옵션을 설정
	        data: { checkboxes: checkboxes },
	        success:function(data){ //통신 성공
	        	console.log("data: " + data)
              if("1" == data) {
                alert("수정되었습니다.");
                location.reload();
              } else {
                alert("수정을 실패했습니다.");
              }
	        },
	        error:function(data){ //실패시 처리
	          console.log("error:"+data);
	        }
	    }); // ajax
    } // if
  } // selectSave()
  
  
////////////////////////////////////////////////////////////////////////////////
// 이미지 모달 숨기기
function hideImageModal() {
  let modal = document.getElementById("imageModal");
  modal.style.display = "none";
}

$(document).ready(function() {
  let name; // 클릭한 이미지의 이름을 저장하는 변수 

  // 이미지 모달 창 클릭 시 호출될 함수 등록
  $(document).on('click', '.uploaded-image', showModalOnClick);
  
  // 모달 창 닫기 클릭 시 호출될 함수 등록
  $(document).on('click', '.image-modal-close', hideImageModal);
  
  // 이미지 모달 창 클릭 시 호출될 함수
  function showModalOnClick() {
    let imageSrc = $(this).attr('src');
    showImageModal(imageSrc);
  }

  // 이미지 크게 보기 창 보이기
  function showImageModal(imageSrc) {
    let modal = document.getElementById("imageModal");
    let modalImage = document.getElementById("modalImage");

    // 이미지 경로 설정에 context path 추가
    modalImage.src = imageSrc;

    modal.style.display = "block";

    // 이미지 상세 조회
    //console.log(imageSrc);
    let firstIdx = imageSrc.lastIndexOf('/') + 1;
    let lastIdx = imageSrc.length;
    name = imageSrc.slice(firstIdx, lastIdx);
    //console.log(name);

    $.ajax({
      type: "GET",
      url: "/doSelectOne",
      asyn: "true",
      dataType: "json",
      data: {
        name: name
      },
      success: function(data) { //통신 성공
        // console.log("success data:"+data);
    	  
    	  let category_val;
    	  if (data.category === 10) {
    		  category_val = "기본"
    	  } else if (data.category === 20) {
    		  category_val = "좋아요"
    	  } else if (data.category === 30) {
          category_val = "싫어요"
    	  }
    	  
      
        $("#idx").text(data.idx);
        $("#name").text(data.name);
        $("#id").text(data.id);
        $("#uploadDate").text(data.uploadDate);
        $("#fileSize").text(data.fileSize);
        $("#category").text(category_val);
        $("#u1").text((data.u1 === 0) ? "오류없음" : "오류있음");
        $("#u2").text((data.u2 === 0) ? "오류없음" : "오류있음");
      },
      error: function(data) { //실패시 처리
        console.log("error:" + data);
      }
    }); // ajax
  } // showImageModal
  
  //상세보기 저장
  $('#detailSaveBtn').on('click', function() {
    console.log("detailSaveBtn click");
    console.log(name);

    if (confirm("저장하시겠습니까?")) {
      $.ajax({
        type: "POST",
        url: "/checkedUpdate",
        asyn: "true",
        dataType: "html",
        data: {
          name: name
        },
        success: function(data) { //통신 성공
          console.log("success data:" + data);
          let parsedJson = JSON.parse(data);
          if ("1" == parsedJson.msgId) {
            alert(parsedJson.msgContents);
            hideImageModal();
            location.reload();
          } else {
            alert(parsedJson.msgContents);
          }
        },
        error: function(data) { //실패시 처리
          console.log("error:" + data);
        }
      }); // ajax
    } // if
  }); // detailSave()

  //상세보기 삭제
  $('#detailDeleteBtn').on('click', function() {
    console.log("detailDeleteBtn click");
    console.log(name);

    if (confirm("삭제하시겠습니까?")) {
      $.ajax({
        type: "GET",
        url: "/doDelete",
        asyn: "true",
        dataType: "html",
        data: {
          name: name
        },
        success: function(data) { //통신 성공
          console.log("success data:" + data);
          let parsedJson = JSON.parse(data);
          if ("1" == parsedJson.msgId) {
            alert(parsedJson.msgContents);
            hideImageModal();
            location.reload();
          } else {
            alert(parsedJson.msgContents);
          }
        },
        error: function(data) { //실패시 처리
          console.log("error:" + data);
        }
      }); // ajax
    } // if
  }); // detailDelete()


});


// 드롭다운으로 카테고리 선택해서 카테고리에 따른 목록 조회
$("#categoryDropdown").change(function() {
  
  let selectedCategory = $(this).val();
  
  window.location.href = "/imgManagement?category=" + encodeURIComponent(selectedCategory);

}); // categoryDropdown
  
</script>
</body>
</html>