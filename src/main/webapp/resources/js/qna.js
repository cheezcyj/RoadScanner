const qnaContextElement = document.querySelector('meta[name="application-context"]');
const qnaContextPath = qnaContextElement ? qnaContextElement.getAttribute('content') : '';
const qnaUrl = function (path) {
    return qnaContextPath + path;
};

const qnaQuestionMode = function () {
    const modeElement = document.getElementById('questionMode');
    return modeElement && modeElement.value === 'inquiry' ? 'inquiry' : 'board';
};

const qnaReturnPath = function (fallbackPath) {
    const returnPathElement = document.getElementById('returnPath');
    const candidate = returnPathElement ? returnPathElement.value : '';
    const allowedPaths = {
        '/qna': true,
        '/qna/my': true,
        '/qna/inquiries': true
    };
    return allowedPaths[candidate] ? candidate : fallbackPath;
};

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
            	_this.delete();
            }
        });

        $('#btn-select-file').on('click', function () {
            $('#attachFile').trigger('click');
        });

        $('#btn-remove-file').on('click', function () {
            $('#attachFile').val('');
            $('#fileText').val('');
            $('#isFileChanged').val('true');
        });

        $('#attachFile').on('change', function (e) {
	        	_this.displaySelectedFile(e);
        });
        
    },
    
    displaySelectedFile : function (event) {
	    	const file = event.target.files[0];
	    	if (file) {
    		// 파일 크기 체크 (5MB)
    		const maxSize = 5 * 1024 * 1024;
	    		if (file.size > maxSize) {
	    			alert('최대 5MB인 이미지만 선택 가능합니다.');
	    			event.target.value = '';
	    			return;
	    		}
    		// 허용된 이미지 확장자 체크
	    		const allowedExtensions = ['jpg', 'jpeg', 'png', 'bmp'];
    		const fileExtension = file.name.split('.').pop().toLowerCase();
	    		if (!allowedExtensions.includes(fileExtension)) {
	    			alert('이미지 파일이 아닙니다.');
	    			event.target.value = '';
	    			return;
	    		}

	    		$('#fileText').val(file.name);
	    		$('#isFileChanged').val('true');
	    	}
    },

    save_file : function () {
	    	if (!this.validateQuestionForm()) {
	    		return;
	    	}
	    	const inquiry = qnaQuestionMode() === 'inquiry';
	    	const saveUrl = inquiry ? '/api/qna/inquiries' : '/api/qna/save';
	    	const returnPath = qnaReturnPath(inquiry ? '/qna/my' : '/qna');
	    	const formData = this.questionFormData();
	    	const input = document.getElementById('attachFile');
	    	if (input && input.files.length > 0) {
	    		formData.append('fileUpload', input.files[0]);
	    	}
	    	this.persistMultipart(qnaUrl(saveUrl), formData, function () {
	    		alert(inquiry ? '문의가 등록되었습니다.' : '글이 등록되었습니다.');
	    		window.location.href = qnaUrl(returnPath);
	    	}, '등록에 실패했습니다.');
    },

    update_file : function () {
	    	if (!this.validateQuestionForm()) {
	    		return;
	    	}
	    	const fileChanged = $('#isFileChanged').val() === 'true';
	    	const selectedFile = $('#attachFile')[0] && $('#attachFile')[0].files.length > 0;
	    	const action = !fileChanged ? 'KEEP' : (selectedFile ? 'REPLACE' : 'REMOVE');
	    	const formData = this.questionFormData();
	    	formData.append('attachmentAction', action);
	    	if (action === 'REPLACE') {
	    		formData.append('fileUpload', $('#attachFile')[0].files[0]);
	    	}

	    	const no = $('#no').val();
	    	this.persistMultipart(qnaUrl('/api/qna/' + no), formData, function () {
	    		alert('글이 수정되었습니다.');
	    		window.location.href = qnaUrl('/qna/' + no);
	    	}, '수정에 실패했습니다.');
    },

    questionFormData : function () {
	    	if (window.roadscannerQnaEditors) {
	    		window.roadscannerQnaEditors.syncAll();
	    	}
	    	const formData = new FormData();
	    	formData.append('category', $('#category').val());
	    	formData.append('title', $('#title').val());
	    	formData.append('content', $('#content').val());
	    	return formData;
    },

    validateQuestionForm : function () {
	    	if (window.roadscannerQnaEditors) {
	    		window.roadscannerQnaEditors.syncAll();
	    	}
	    	const content = document.getElementById('content');
	    	if (content && content.validationMessage) {
	    		alert(content.validationMessage);
	    		const surface = document.querySelector('[data-qna-editor] .qna-editor-surface');
	    		if (surface) {
	    			surface.focus();
	    		}
	    		return false;
	    	}
	    	return true;
    },

    persistMultipart : function (url, formData, onSuccess, failureMessage) {
	    	$.ajax({
	    		type: 'POST',
	    		url: url,
	    		dataType: 'json',
	    		processData: false,
	    		contentType: false,
	    		data: formData
	    	}).done(onSuccess).fail(function (error) {
	    		alert(failureMessage);
	    		console.error('Question request failed');
	    	});
    },

    delete : function () {
        const no = $('#no').val();
		const inquiry = qnaQuestionMode() === 'inquiry';
		const returnPath = qnaReturnPath(inquiry ? '/qna/my' : '/qna');

        $.ajax({
            type: 'DELETE',
            url: qnaUrl('/api/qna/' + no),
        }).done(function () {
            alert(inquiry ? '문의가 삭제되었습니다.' : '글이 삭제되었습니다.');
            window.location.href = qnaUrl(returnPath);
        }).fail(function (error) {
            alert('글 삭제 실패했습니다.');
            console.error('Question deletion failed');
        });
    }
};

main.init();


const answer = {
    init: function() {
        const _this = this;
        const originalUpdateContent = $('#answer-update-content').val();
        $('#btn-answer-save').on('click', function(e) {
            e.preventDefault();
            _this.save();
        });

        $('#btn-answer-delete').on('click', function() {
           if (confirm('정말 삭제하시겠습니까?')) {
               _this.delete();
           }
        });


        $('#btn-answer-update-form').on('click', function(e) {
            e.preventDefault();
            $('#answer-update-form').css('display', 'block');
            $('#answer-detail').css('display', 'none');
        });


        $('#btn-answer-updated').on('click', function(e) {
            e.preventDefault();
            _this.update();
        });

        $('#btn-answer-cancel-update').on('click', function(e) {
            e.preventDefault();
            $('#answer-update-content').val(originalUpdateContent);
            $('#answer-update-form').css('display', 'none');
            $('#answer-detail').css('display', 'block');
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
            url: qnaUrl('/api/qna/' + no + '/answer'), // 답변 등록 API 엔드포인트
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            data: JSON.stringify(answerData),
        }).done(function() {
            alert('답변이 등록되었습니다.');
            // 답변 등록 후, 답변을 다시 불러와서 화면 갱신
            window.location.href = qnaUrl('/qna/' + no);
        }).fail(function(error) {
            alert('답변 등록에 실패했습니다.');
            console.error('Answer creation failed');
        });
    },


    delete : function(){
        const no = $('#no').val();

        $.ajax({
            type: 'DELETE',
            url: qnaUrl('/api/qna/' + no + '/answer'),
        }).done(function () {
            alert('답변이 삭제되었습니다.');
            window.location.href = qnaUrl('/qna/' + no);
        }).fail(function (error) {
            alert('답변 삭제에 실패했습니다.');
            console.error('Answer deletion failed');
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

        // 답변 등록 Ajax 요청
        $.ajax({
            type: 'PUT',
            url: qnaUrl('/api/qna/' + no + '/answer'), // 답변 등록 API 엔드포인트
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            data: JSON.stringify(answerData),
        }).done(function() {
            alert('답변이 수정되었습니다.');
            // 답변 수정 후, 답변을 다시 불러와서 화면 갱신
            window.location.href = qnaUrl('/qna/' + no);
        }).fail(function(error) {
            alert('답변 수정에 실패했습니다.');
            console.error('Answer update failed');
        });
    }

};

// 초기화 함수 호출
// main.init();
answer.init();
