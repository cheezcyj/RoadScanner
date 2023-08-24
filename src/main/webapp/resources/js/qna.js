$(document).ready(function () {
    // 파일 선택 버튼을 클릭하면 숨겨진 파일 입력 필드를 클릭합니다.
    $('#btn-select-file').click(function () {
        $('#attachFile').click();
        $('#isFileChanged').val('true'); // boolean 사용
    });

    // 파일이 선택되면 파일 이름을 텍스트 입력 필드에 표시합니다.
    $('#attachFile').change(function () {
        var fileName = $(this).val().split('\\').pop(); // 파일 경로에서 파일 이름만 가져옵니다.
        $('#fileText').val(fileName);
    });

    // 삭제 버튼을 클릭하면 파일 입력 필드와 텍스트 입력 필드를 초기화합니다.
    $('#btn-remove-file').click(function () {
        // $('#attachFile').replaceWith($('#attachFile').clone(true));
        $('#attachFile').val('');
        $('#fileText').val(''); // 텍스트 입력 필드를 초기화합니다.
        $('#isFileChanged').val('true'); // boolean 사용
    });
});

// CKEDITOR 관련 초기화
CKEDITOR.replace('content', {
    // 툴바 설정을 통해 원하는 버튼만 포함
    toolbar: [
        { name: 'basicstyles', items: [ 'Bold', 'Italic', 'Underline', 'Strike' ] },
        { name: 'paragraph', items: [ 'NumberedList', 'BulletedList', 'JustifyLeft', 'JustifyCenter', 'JustifyRight', 'JustifyBlock' ] },
        { name: 'links', items: [ 'Link', 'Unlink' ] },
        { name: 'styles', items: [ 'Format', 'Font', 'FontSize' ] }
    ],
    // 필요하지 않은 플러그인 제거
    removePlugins: 'image,flash,tabletools,smiley'
});

const main = {

    init: function () {

        const _this = this;
        $('#btn-save').on('click', function (e) {
            e.preventDefault();
            _this.save();
        });

        $('#btn-update').on('click', function (e) {
            e.preventDefault();
            _this.update();
        });

        $('#btn-delete').on('click', function () {
            if (confirm('정말 삭제하시겠습니까?')) {
                _this.delete();
            }
        });

        // 이미지 삭제
        // $('#btn-remove-file').on('click', function(e) {
        //     e.preventDefault(); // 기본 동작을 막음
        //     $('#attachFile').val(''); // 파일 입력 필드를 초기화
        // });

        $('#btn-delete-selected').on('click', function () {
            // console.log('select button clicked');
            if (confirm('선택한 항목을 삭제하시겠습니까?')) {
                const checkedItems = $('.delete-checkbox:checked');
                    const totalItems = checkedItems.length;
                    let deletedItems = 0;

                checkedItems.each(function(index) {
                    const no = $(this).val();
                    _this.deleteItem(no, function() {
                        deletedItems++;
                        if (deletedItems === totalItems) {
                            alert('선택한 항목이 삭제되었습니다.');
                            window.location.reload();
                        }
                    });
                });
            }
        });

        $('#select-all').on('click', function() {
            // #select-all이 체크되면 모든 .delete-checkbox의 상태를 #select-all과 동일하게 설정
            $('.delete-checkbox').prop('checked', $(this).prop('checked'));
            showDeleteBtn();
        });

        $('.delete-checkbox').on('change', function() {
            showDeleteBtn();
        });

        // 페이지 로드시 체크박스 선택 상태에 따른 버튼 표시 상태 초기화
        showDeleteBtn();

        $('#btn-toggle-notice').on('click', function () {
            $('.notice-row').toggle(); // 공지 테이블의 표시/숨김을 전환

            const isNoticeVisible = $('.notice-row').is(':visible');

            // 로컬 스토리지에 토글 상태를 저장(테이블이 표시되면 'show', 숨겨지면 'hide')
            sessionStorage.setItem('notice-toggle', isNoticeVisible ? 'show' : 'hide');

            // 버튼의 텍스트를 변경. 테이블이 표시되면 '공지감추기', 숨겨지면 '공지보이기'
            $(this).text(isNoticeVisible ? '공지 숨기기' : '공지 보기');
        });

        // 페이지가 로드될 때 세션 스토리지에서 토글 상태를 불러와 설정
        const toggleStatus = sessionStorage.getItem('notice-toggle');
        if (toggleStatus === 'hide') {
            $('.notice-row').hide();
            $('#btn-toggle-notice').text('공지 보기'); // 페이지 로딩 시 버튼의 텍스트를 맞게 설정
        } else {
            $('#btn-toggle-notice').text('공지 숨기기'); // 페이지 로딩 시 버튼의 텍스트를 맞게 설정
        }
    },

    /**
     * 게시글 작성과 관련됨.
     */

    save: function () {
        if (!validateForm()) return; // 검증 실패 시 함수 종료

        const data = new FormData();
        data.append('category', $('#category').val());
        data.append('id', $('#id').val());
        data.append('title', $('#title').val());

        const content = CKEDITOR.instances.content.getData();
        // data.append('content', $('#content').val());
        data.append('content', content);

        const file = $('#attachFile')[0].files[0];
        if (file) { // 파일이 존재하는 경우에만 추가
            data.append('attachFile', file);
        }
        
        $.ajax({
            type: 'POST',
            enctype: 'multipart/form-data', // 이 부분 추가
            url: '/api/qna/save',
            processData: false,
            contentType: false,
            data: data
        }).done(function () {
            alert('글이 등록되었습니다.');
            window.location.href = '/qna';
        }).fail(function (error) {
            const errors = error.responseJSON; // 서버에서 전달된 에러메시지
            alert("등록 실패했습니다.");
            // 제목 오류 메시지 처리

            if (errors.title) { // 'error'가 아닌 'errors'를 사용
                $('#title').addClass('field-error'); // input 필드에 클래스 추가
                $('#title-error').text(errors.title).addClass('field-error');
            }

            // 내용 오류 메시지 처리
            if (errors.content) {
                $('#content').addClass('field-error');
                $('#content-error').text(errors.content).addClass('field-error');
            }

            // 이미지 파일 오류 메시지 처리
            if (errors.attachFile) {
                $('#attachFile').addClass('field-error');
                $('#attachFile-error').text(errors.attachFile).addClass('field-error');
            }
        })
        console.error(error);
    },

    update : function () {

        if (!validateForm()) return; // 검증 실패 시 함수 종료

        const data = new FormData();
        data.append('no', $('#no').val());
        data.append('category', $('#category').val());
        data.append('id', $('#id').val());
        data.append('title', $('#title').val());

        const content = CKEDITOR.instances.content.getData();
        // data.append('content', $('#content').val());
        data.append('content', content);

        const isFileChanged = $('#isFileChanged').val();
        alert(isFileChanged);
        data.append('isFileChanged', isFileChanged);

        const file = $('#attachFile')[0].files[0];
        if (file) { // 파일이 존재하는 경우에만 추가
            data.append('attachFile', file);
        }

        const no = $('#no').val();

        $.ajax({
            type: 'POST',
            enctype:'multipart/form-data',
            url: '/api/qna/' + no,
            processData: false,
            contentType: false,
            data: data
        }).done(function () {
            alert("글이 수정되었습니다.");
            window.location.href = '/qna/' + no; // 수정된 페이지로 리다이렉트
        }).fail(function (error) {
            alert("수정 실패했습니다.");
            const responseJSON = JSON.parse(error.responseText);
            responseJSON.forEach(function (errorMessage) {
                alert(errorMessage);
            });
        });
    },

    delete: function () {
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
    },

    deleteItem: function (no, callback) {
        $.ajax({
            type: 'DELETE',
            url: '/api/qna/' + no,
        }).done(function () {
            if (callback) callback();
        }).fail(function (error) {
            alert('삭제에 실패했습니다.');
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

            // .fail(function(jqXHR) {
            //     var response = JSON.parse(jqXHR.responseText);
            //     alert(response.message); // 서버에서 보낸 메시지를 표시
            // })
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

function validateForm() {
    // 제목 길이 검증
    const title = $('#title').val();
    if (title.length > 15) {
        alert('제목은 15글자 이하여야 합니다.');
        $('#title').focus(); // 제목 입력 필드에 포커스
        return false;
    }

    // 제목과 내용이 비어있는지 검증
    if (!title) {
        alert('제목은 필수입니다.');
        $('#title').focus(); // 제목 입력 필드에 포커스
        return false;
    }

    // const content = $('#content').val();
    const content = CKEDITOR.instances.content.getData();
    if (!content) {
        alert('내용은 필수입니다.');
        // $('#content').focus(); // 내용 입력 필드에 포커스
        CKEDITOR.instances.content.focus(); // CKEditor 내용 부분에 포커스
        return false;
    }

    // 이미지 파일 검증
    const file = $('#attachFile')[0].files[0];
    if (file) {
        if (!file.type.match('image.*')) {
            alert('유효한 이미지 파일만 업로드할 수 있습니다.');
            $('#attachFile').focus(); // 파일 입력 필드에 포커스
            return false;
        }

        // 파일 크기 검증 (5MB 이하)
        const fileSizeMB = file.size / (1024 * 1024);
        if (fileSizeMB > 5) {
            alert('파일 크기는 5MB 이하여야 합니다.');
            $('#attachFile').focus(); // 파일 입력 필드에 포커스
            return false;
        }
    }

    return true;
}

function showDeleteBtn() {
     if ($('.delete-checkbox:checked').length > 0) {
         $('#btn-delete-selected').show();
     } else {
         $('#btn-delete-selected').hide();
     }
 }
