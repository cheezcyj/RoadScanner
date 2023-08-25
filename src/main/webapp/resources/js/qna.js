const main = {
    init: function () {
        const _this = this;
        $('#btn-save').on('click', function (e) {
            e.preventDefault();
            _this.save_file();
        });

        $('#btn-update').on('click', function (e) {
            e.preventDefault();
            _this.update_file();
        });

        $('#btn-delete').on('click', function () {
            if (confirm('정말 삭제하시겠습니까?')) {
            	_this.delete_file();
            }
        });
        
        $('#cancelButton').on('click', function () {
        	const thisFile = document.getElementById('thisFile');
        	const uploadLabel = document.getElementById('uploadLabel');
        	const fileUpload = document.getElementById('fileUpload');
        	const fileName = document.getElementById('fileName');
        	
        	thisFile.style.display = 'none';
        	fileUpload.value = '';
        	fileName.value = '';
        	uploadLabel.style.display = 'block';
        	
        });
        
        $('#fileUpload').on('change', function (e) {
        	const count = document.getElementById('count');
        	count.value = '1';
        	_this.displaySelectedFile(e);
        });
        
    },
    
    displaySelectedFile : function (event) {
    	const cancelButton = document.getElementById('cancelButton');
    	const fileUpload = document.getElementById('fileUpload');
    	const uploadLabel = document.getElementById('uploadLabel');
    	const selectedName = fileUpload.files[0].name;
    	const thisFile = document.getElementById('thisFile');
    	const fileName = document.getElementById('fileName');
    	
    	const file = event.target.files[0];
    	if (file) {
    		// 파일 크기 체크 (5MB)
    		const maxSize = 5 * 1024 * 1024;
    		if (file.size > maxSize) {
    			alert('최대 5MB인 이미지만 선택 가능합니다.');
    			fileUpload.value = '';
    			return;
    		}
    		// 허용된 이미지 확장자 체크
    		const allowedExtensions = ['jpg', 'jpeg', 'png', 'bmp', 'tiff', 'webp', 'ico', 'svg'];
    		const fileExtension = file.name.split('.').pop().toLowerCase();
    		if (!allowedExtensions.includes(fileExtension)) {
    			alert('이미지 파일이 아닙니다.');
    			fileUpload.value = '';
    			return;
    		}
    		
    		const reader = new FileReader();
    		reader.readAsDataURL(file);                 // 파일을 base64로 읽기(파일명)
    		reader.onload = function() {
    			//console.log("name: "+selectedName);
    			uploadLabel.style.display = 'none';     // 파일선택 버튼 숨기기
    			cancelButton.style.display = 'block';   // 취소버튼 보이기
    			fileName.value = selectedName;          // 파일명 교체
    			thisFile.style.display = 'block';       // 파일명 띄우기
    		};
    	}
    },
    
    save_file : function () {
    	const fileName = document.getElementById('fileName'); //첨부파일명
    	//console.log(fileName.value);
    	
    	if (fileName.value != '') {
    		let formData = new FormData(); // FormData 생성
    		formData.append("fileUpload", $("#fileUpload")[0].files[0]); // 파일 파트 추가
    		formData.append("idx", 1);
    		formData.append("id", $("#id").val());
    		formData.append("category", 40);
    		formData.append("name", "testname");
    		formData.append("url", "testurl");
    		formData.append("fileSize", 0);
    		formData.append("checked", 0);
    		formData.append("u1", 0);
    		formData.append("u2", 0);
    		
    		// 파일 저장
    		$.ajax({
    			type: "POST",
    			url: "/qna/fileUploaded",
    			processData: false,
    			contentType: false,
    			data: formData,
    			success: function (data) {
    				//console.log(data);
    				$("#idx").val(parseInt(data, 10));
    				// 파일 업로드 성공 후에 save 함수 호출
    				main.save();
    			},
    			error: function (data) {
    				console.log(data);
    				console.error("파일 업로드 오류");
    				return;
    			},
    		});
    	} else {
    		main.save();
    	}
    	
    },
    
    update_file : function () {
    	//console.log("update_file");
    	const fileName = document.getElementById('fileName'); //수정파일명
    	const thisName = document.getElementById('thisName'); //기존파일명
    	const count = document.getElementById('count');
    	//console.log(thisName.value);
    	
    	// 기존파일 처리(삭제/유지)
		if (thisName.value != '') {  //기존 글에 파일 첨부 O
			if (count.value == '1' || fileName.value == '') { //파일이 변경되었거나 새로 첨부되지 않았을 경우
				$.ajax({
					type: "GET",
					url: "/qna/fileDelete",
					asyn: "true",
					dataType: "html",
					data: {
						name: thisName.value
					},
					success: function(data) {
					},
					error: function(data) {
						console.log(data);
						console.error("기존파일 삭제 오류");
					}
				});
			} else { //기존파일 그대로일경우(글만 수정)
				console.log("파일 교체 없음");
			}
		}
    	
		// 신규파일 처리 (등록 여부)
    	if (fileName.value != '') { //수정된 글에 파일 첨부 O
    		if (count.value == '1') { //파일이 변경되었을 경우
    			//console.log("fileName.value != null");
    			let formData = new FormData(); // FormData 생성
    			formData.append("fileUpload", $("#fileUpload")[0].files[0]); // 파일 파트 추가
    			formData.append("idx", 1);
    			formData.append("id", $("#id").val());
    			formData.append("category", 40);
    			formData.append("name", "testname");
    			formData.append("url", "testurl");
    			formData.append("fileSize", 0);
    			formData.append("checked", 0);
    			formData.append("u1", 0);
    			formData.append("u2", 0);
    			
    			// 신규 파일 저장
    			$.ajax({
    				type: "POST",
    				url: "/qna/fileUploaded",
    				processData: false,
    				contentType: false,
    				data: formData,
    				success: function (data) {
    					//console.log("fileUploaded success");
    					$("#idx").val(parseInt(data, 10));
    					main.update();
    				},
    				error: function (data) {
    					console.log(data);
    					console.error("파일 업로드 오류");
    				},
    			});
    		} else { //기존파일 그대로일경우(글만 수정)
    			console.log("파일 교체 없음");
    			main.update();
    		}
    	} else { //수정된 글에 파일이 없을 경우
    		//console.log("fileName.value: "+fileName.value);
    		$("#idx").val(null);
    		main.update();
    	}
    	
    },
    
    delete_file : function () {
    	const detailImageUrl = document.getElementById('detailImage').src;
    	if (detailImageUrl != null) {
    		const url = detailImageUrl.substring(64);
    		//console.log(url);
			$.ajax({
				type: "GET",
				url: "/qna/fileDelete",
				asyn: "true",
				dataType: "html",
				data: {
					name: url
				},
				success: function(data) {
					main.delete();
				},
				error: function(data) {
					console.log(data);
					console.error("파일 삭제 오류");
				}
			});
    	}
    },
    
    save : function () {
        const data = {
            category: $('#category').val(),
            id: $('#id').val(),
            idx: $('#idx').val(),
            title: $('#title').val(),
            content: $('#content').val(),
        };

        $.ajax({
            type: 'POST',
            url: '/api/qna/save',
            dataType: 'json',
            contentType: "application/json; charset=utf-8",
            data: JSON.stringify(data)
        }).done(function () {
            alert('글이 등록되었습니다.');
            window.location.href = '/qna';
        }).fail(function (error) {
            alert("등록 실패했습니다.");
            console.error(error);
            // alert(JSON.stringify(error));
        });
    },

    update : function () {
        const data = {
            category: $('#category').val(),
            title: $('#title').val(),
            idx: $('#idx').val(),
            content: $('#content').val(),
        };

        const no = $('#no').val();

        $.ajax({
            type: 'PUT',
            url: '/api/qna/' + no,
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            data: JSON.stringify(data)
        }).done(function () {
            alert("글이 수정되었습니다.");
            window.location.href = '/qna/' + no; // 수정된 페이지로 리다이렉트
        }).fail(function (error) {
            alert(JSON.stringify(error));
        });
    },

    delete : function () {
        const no = $('#no').val();

        $.ajax({
            type: 'DELETE',
            url: '/api/qna/' + no,
        }).done(function () {
            alert('글이 삭제되었습니다.');
            window.location.href = '/qna';
        }).fail(function (error) {
            alert('글 삭제 실패했습니다.');
            console.error(error);
        });
    }
};

main.init();


const answer = {
    init: function() {
        const _this = this;
        $('#btn-answer-save').on('click', function(e) {
            e.preventDefault();
            _this.save();
        });

        $('#btn-answer-delete').on('click', function() {
            // console.log('click delete');
           if (confirm('정말 삭제하시겠습니까?')) {
               _this.delete();
           }
        });


        $('#btn-answer-update-form').on('click', function(e) {
            // console.log('click update-form');
            e.preventDefault();
            $('#answer-update-form').css('display', 'block');
            $('#answer-detail').css('display', 'none');
        });


        $('#btn-answer-updated').on('click', function(e) {
            // console.log('click update');
            e.preventDefault();
            _this.update();
        });

    },

    save : function() {
        const no = $('#no').val();

        // 사용자가 입력한 답변 내용
        const answerData = {
            no: no,
            id: $('#id').val(),
            content: $('#answer-content').val()
        };

        // 입력 내용 검사
        if (!answerData.content) {
            alert('답변 내용을 입력해주세요.');
            $('#answer-content').focus(); // 커서를 답변 내용 입력 필드로 이동
            return;
        }

        // 답변 등록 Ajax 요청
        $.ajax({
            type: 'POST',
            url: '/api/qna/' + no + '/answer', // 답변 등록 API 엔드포인트
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            data: JSON.stringify(answerData),
        }).done(function() {
            alert('답변이 등록되었습니다.');
            // 답변 등록 후, 답변을 다시 불러와서 화면 갱신
            window.location.href = '/qna/' + no;
        }).fail(function(error) {
            alert('답변 등록에 실패했습니다.');
            console.error(error + answerData);
        });
    },


    delete : function(){
        const no = $('#no').val();

        $.ajax({
            type: 'DELETE',
            url: '/api/qna/' + no + '/answer',
        }).done(function () {
            alert('답변이 삭제되었습니다.');
            window.location.href = '/qna/' + no;
        }).fail(function (error) {
            alert('답변 삭제에 실패했습니다.');
            console.error(error);
        });
    },

    update : function () {
        const no = $('#no').val();

        const answerData = {
            content: $('#answer-update-content').val()
        };

        // 입력 내용 검사
        if (!answerData.content) {
            alert('답변 내용을 입력해주세요.');
            $('#answer-update-content').focus(); // 커서를 답변 내용 입력 필드로 이동
            return;
        }
s
        // 답변 등록 Ajax 요청
        $.ajax({
            type: 'PUT',
            url: '/api/qna/' + no + '/answer', // 답변 등록 API 엔드포인트
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            data: JSON.stringify(answerData),
        }).done(function() {
            alert('답변이 수정되었습니다.');
            // 답변 수정 후, 답변을 다시 불러와서 화면 갱신
            window.location.href = '/qna/' + no;
        }).fail(function(error) {
            alert('답변 수정에 실패했습니다.');
            console.error(error + answerData);
        });
    }

};

// 초기화 함수 호출
// main.init();
answer.init();
