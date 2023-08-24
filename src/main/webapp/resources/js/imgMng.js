  // 체크박스 클릭 시 이미지에 스타일 추가
  $('.btn_check').on('change', function () {
    if ($(this).is(':checked')) {
      $(this).closest('.image-container').addClass('selected');
    } else {
      $(this).closest('.image-container').removeClass('selected');
    }
  });

  
  /* 전체 선택 */
  function toggleSelectAll() {
    let checkboxes = document.querySelectorAll(".btn_check");
    let selectAllBtn = document.getElementById("selectAllBtn");
    let checked = selectAllBtn.checked;
    
    for (let i = 0; i < checkboxes.length; i++) {
      checkboxes[i].checked = checked;
      let imageContainer = checkboxes[i].closest('.image-container');
      if (checked) {
        imageContainer.classList.add('selected');
      } else {
        imageContainer.classList.remove('selected');
      }
    } // for
  } // toggleSelectAll()


  /* 선택 삭제 */
  function selectDelete() {     
    let checkboxes = [];
    
    // 선택된 체크박스의 idx 추출
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
 
  
  /* 선택 저장 */
  function selectSave() {
    let checkboxes = [];
   
    // 선택된 체크박스의 idx 추출
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




  /* 드롭다운으로 카테고리 선택 */
  $("#categoryDropdown").change(function() {
    let selectedCategory = $(this).val();
    window.location.href = "/imgManagement?pageNo=1&category=" + selectedCategory;
  }); // categoryDropdown()


  let name; // 클릭한 이미지의 이름을 저장하는 변수 
  
  /* 모달 창 닫기 클릭 시 호출될 함수 등록 */
  $(document).on('click', '.btn-close', hideImageModal);
  
  /* esc 키 눌렀을 때 모달 창 닫기 */
  $(document).on('keydown', function (e) {
    if (e.key === "Escape") {
      hideImageModal();
    }
  });
  
  /* 이미지 모달 숨기기 */
  function hideImageModal() {
    let modal = document.getElementById("imageModal");
    let overlay = document.getElementById("overlay-modal");
    modal.style.display = "none";
    overlay.classList.remove("show-overlay");
  } // hideImageModal()
  
  /* 이미지 모달 창 클릭 시 호출될 함수 등록 */
  $(document).on('click', '.uploaded-image', showModalOnClick);
  
  /* 이미지 모달 창 클릭 시 호출될 함수 */
  function showModalOnClick() {
    let imageSrc = $(this).attr('src');
    showImageModal(imageSrc);
  } // showModalOnClick()

  /* 이미지 크게 보기 창 보이기 */
  function showImageModal(imageSrc) {
    let modal = document.getElementById("imageModal");
    let modalImage = document.getElementById("modalImage");
    let overlay = document.getElementById("overlay-modal");
    
    modal.style.display = "block"; // 모달 창 보이기
    overlay.classList.add("show-overlay"); // 모달 창 밖 overlay 효과

    // 이미지 경로 설정에 context path 추가
    modalImage.src = imageSrc;

    // 이미지 상세 조회
    let firstIdx = imageSrc.lastIndexOf('/') + 1;
    let lastIdx = imageSrc.length;
    name = imageSrc.slice(firstIdx, lastIdx); // url에서 이름 추출

    $.ajax({
      type: "GET",
      url: "/doSelectOne",
      asyn: "true",
      dataType: "json",
      data: {
        name: name
      },
      success: function(data) { //통신 성공
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
        $("#u1").text((data.u1 === 0 || data.u1 === null) ? "오류없음" : "오류있음");
        $("#u2").text((data.u2 === 0 || data.u1 === null) ? "오류없음" : "오류있음");
        $("#u3").text((data.u3 === 0 || data.u1 === null) ? "오류없음" : "오류있음");
      },
      error: function(data) { //실패시 처리
        console.log("error:" + data);
      }
    }); // ajax
  } // showImageModal()
  
  
  /* 상세보기 저장 */
  $('#detailSaveBtn').on('click', function() {
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

  
  /* 상세보기 삭제 */
  $('#detailDeleteBtn').on('click', function() {
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
