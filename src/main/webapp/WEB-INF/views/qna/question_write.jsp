<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>게시판 글쓰기</title>

    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
<header class="bg-light text-center p-4">
    <h1>로드스캐너</h1>
</header>

    <div class="container my-4">
    
        <form>
            <h2 class="mb-4">Q&A 게시판</h2>

            <div class="mb-3 row">
                <label for="postTitle" class="col-sm-2 col-form-label">제목</label>
                <div class="col-sm-10">
                    <input type="text" class="form-control" id="postTitle" value="${question.title}">
                </div>
            </div>
            
            <div class="mb-3 row">
                <label for="author" class="col-sm-2 col-form-label">작성자</label>
                <div class="col-sm-10">
                    <input type="text" class="form-control" id="author" value="${question.id}" readonly>
                </div>
            </div>
            
            <div class="mb-3 row">
                <label for="attachment" class="col-sm-2 col-form-label">첨부파일</label>
                <div class="col-sm-10">
                    <input type="file" class="form-control" id="attachment">
                </div>
            </div>
            
            <div class="mb-3 row">
                <label for="content" class="col-sm-2 col-form-label">내용</label>
                <div class="col-sm-10">
                    <textarea class="form-control" id="content" rows="10">${question.content}</textarea>
                </div>
            </div>
            
            <div class="text-center">
                <button type="button" class="btn btn-secondary me-2" onclick="location.href='/qna/list';">취소</button>
                <button type="submit" class="btn btn-primary me-2">작성완료</button>
            </div>
        </form>
    </div>

	<!-- JavaScript -->
	<script>
	    function validateForm() {
	        var postTitle = document.getElementById("postTitle").value;
	        var content = document.getElementById("content").value;
	        
	        if (postTitle.trim() === "" || content.trim() === "") {
	            alert("제목과 내용을 입력해주세요.");
	            return false; // 폼 제출 중지
	        }
	        return true; // 폼 제출 진행
	    }
	</script>

<!-- Bootstrap JS -->
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>