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
    },

    save: function () {
        const data = new FormData();
        data.append('category', $('#category').val());
        data.append('id', $('#id').val());
        data.append('title', $('#title').val());
        data.append('content', $('#content').val());

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
            alert("등록 실패했습니다.");
            console.error(error);
        });
    },

    update : function () {
        const data = new FormData();
        data.append('no', $('#no').val());
        data.append('category', $('#category').val());
        data.append('id', $('#id').val());
        data.append('title', $('#title').val());
        data.append('content', $('#content').val());

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
            alert(JSON.stringify(error));
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
