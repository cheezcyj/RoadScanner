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
    },

    save: function() {
        const no = $('#no').val();

        // 사용자가 입력한 답변 내용
        const answerData = {
            no: no,
            id: $('#id').val(),
            content: $('#answer-content').val()
        };

        // 답변 등록 Ajax 요청
        $.ajax({
            type: 'POST',
            url: '/api/qna/' + no + '/saveAnswer', // 답변 등록 API 엔드포인트
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            data: JSON.stringify(answerData),
        }).done(function() {
            alert('답변이 등록되었습니다.');
            // 답변 등록 후, 답변 목록을 다시 불러와서 화면 갱신
            window.location.href = '/qna/' + no;
        }).fail(function(error) {
            alert('답변 등록에 실패했습니다.');
            console.error(error + answerData);
        });
    },

    load: function() {
            // Ajax 요청을 통해 댓글을 불러오고 HTML에서 댓글 섹션을 업데이트
            $.get('/api/qna/' + no + '/answer', function(data) {
                // 데이터를 처리하고 HTML에서 댓글 섹션을 업데이트
                $('#answer-section').html(data);
            });
    }
};

// 초기화 함수 호출
answer.init();
