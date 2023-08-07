<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="ko">

<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css" rel="stylesheet">

    <title>게시판 상세페이지</title>
</head>

<body>
    <header class="bg-light text-center p-4">
        <h1>로드스캐너</h1>
    </header>

    <div class="container my-4">

        <form>
            <h2 class="mb-4">Q&A 게시판</h2>

            <div class="mb-3">
                <input class="form-control" type="text" value="제목" aria-label="Disabled input example" disabled readonly>
            </div>

            <div class="row mb-3">
                <div class="col">작성자:</div>
                <div class="col-md-auto">작성일:</div>
                <div class="col-md-auto">조회수:</div>
            </div>

            <div class="mb-3">
                <textarea class="form-control" aria-label="Disabled input example" disabled readonly rows="5">내용</textarea>
            </div>

            <div class="d-grid gap-2 d-md-flex justify-content-md-end mb-3">
                <button class="btn btn-secondary btn-sm me-2" type="button">삭제</button>
                <button class="btn btn-secondary btn-sm" type="button">수정</button>
            </div>

            <div class="text-center mb-3">
                <button type="button" class="btn btn-primary me-2" onclick="location.href='/';">목록</button>
                <button type="button" class="btn btn-primary me-2" onclick="location.href='/';">게시글쓰기</button>
                <button type="button" class="btn btn-primary" onclick="showAnswerForm()">답변하기</button>
            </div>
        </form>

        <div style="padding: 10px 0;"></div>

        <!-- 답변  -->
        <div id="answerSection"></div>
        <form id="answerForm" style="display: none;">
            <h3 class="mb-4">답변 작성</h3>

            <div class="mb-3">
                <label for="answerName" class="form-label">답변 관리자 이름</label>
                <input type="text" class="form-control" id="answerName" required>
            </div>

            <div class="mb-3">
                <label for="answerContent" class="form-label">내용</label>
                <textarea class="form-control" id="answerContent" rows="5" required></textarea>
            </div>

            <div class="text-center">
                <button type="button" class="btn btn-primary" onclick="submitAnswer()">답변 등록</button>
            </div>
        </form>
    </div>

    <!-- Bootstrap JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js"></script>

    <script>
        function showAnswerForm() {
            document.getElementById("answerForm").style.display = "block";
        }

        function submitAnswer() {
            const answerName = document.getElementById("answerName").value;
            const answerContent = document.getElementById("answerContent").value;

            if (answerName && answerContent) {
                const answerSection = document.getElementById("answerSection");
                const answerForm = document.getElementById("answerForm");

                // 답변 내용 생성 및 답변 섹션에 추가
                const answerElement = document.createElement("div");
                answerElement.classList.add("mb-3");
                answerElement.innerHTML = `
                    <h2>등록된 답변</h2>
                    <div style="padding: 10px 0;"></div>
                    <h6>답변 관리자: ${answerName}</h6>
                    <textarea class="form-control" aria-label="Disabled input example" disabled readonly rows="5">${answerContent}</textarea>
                    <div style="padding: 8px 0;"></div>
                    <div class="d-grid gap-2 d-md-flex justify-content-md-end mb-3">
                        <button class="btn btn-secondary btn-sm me-2" type="button">삭제</button>
                        <button class="btn btn-secondary btn-sm" type="button">수정</button>
                    </div>
                `;

                answerSection.appendChild(answerElement);

                // 입력 필드 초기화 및 답변 작성 폼 숨기기
                document.getElementById("answerName").value = "";
                document.getElementById("answerContent").value = "";
                answerForm.style.display = "none";
            }
        }

        function editAnswer(btn) {
            const answerElement = btn.parentElement.parentElement;
            const answerName = answerElement.querySelector("h3").innerText;
            const answerContent = answerElement.querySelector("textarea").value;

            // 수정할 답변의 이름과 내용을 답변 작성 폼에 세팅
            document.getElementById("answerName").value = answerName;
            document.getElementById("answerContent").value = answerContent;

            // 답변 작성 폼 보이기
            document.getElementById("answerForm").style.display = "block";

            // 기존 답변 삭제
            answerElement.remove();
        }

        function deleteAnswer(btn) {
            const answerElement = btn.parentElement.parentElement;
            answerElement.remove();
        }
    </script>

</body>

</html>