<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="ko">

<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css" rel="stylesheet">

    <title>게시글 조회</title>
</head>

<body>
    <header class="bg-light text-center p-4">
        <h1>로드스캐너</h1>
    </header>

    <div class="container my-4">

        <form>
            <h2 class="mb-4">Q&A 게시판</h2>
            
            <input type="hidden" id="no" value="${question.no}">
            <div class="mb-3">
                <input class="form-control" type="text" value="${question.title}" aria-label="Disabled input example" disabled readonly>
            </div>

            <div class="row mb-3">
                <div class="col">작성자: ${question.id}</div>
                <div class="col-md-auto">작성일: ${question.createDate}</div>
                <div class="col-md-auto">조회수: ${question.views}</div>
            </div>

            <div class="mb-3">
                <textarea class="form-control" aria-label="Disabled input example" disabled readonly rows="5">${question.content}</textarea>
            </div>

            <div class="d-grid gap-2 d-md-flex justify-content-md-end mb-3">
                <button class="btn btn-secondary btn-sm me-2" type="button">삭제</button>
                <a href="/qna/update/${question.no}" class="btn btn-secondary btn-sm" type="button">수정</a>
            </div>

            <div class="text-center mb-3">
                <button type="button" class="btn btn-primary me-2" onclick="location.href='/qna';">목록</button>
                <button type="button" class="btn btn-primary me-2" onclick="location.href='/write';">게시글쓰기</button>
                <div class="d-grid gap-2 d-md-flex justify-content-md-end mb-3">
                <!-- 답변하기 버튼을 답변 개수가 1개 미만일 때만 활성화합니다 -->
                <button class="btn btn-primary" type="button" onclick="showAnswerForm()" id="answerButton">답변하기</button>
                </div>
            </div>
        </form>

        <div style="padding: 10px 0;"></div>

        <!-- 답변  -->
        <div id="answerSection"></div>
        <form id="answerForm" style="display: none;">
            <h3 class="mb-4">답변 작성</h3>

            <div class="mb-3">
                <label for="answerName" class="form-label">답변 관리자 이름</label>
                <input type="text" class="form-control" id="answerName" value="${answer.id}" required>
            </div>

            <div class="mb-3">
                <label for="answerContent" class="form-label">내용</label>
                <textarea class="form-control" id="answerContent" rows="5" required></textarea>
            </div>

            <div class="text-center">
                <button type="button" class="btn btn-primary" onclick="submitAnswer()">등록</button>
            </div>
        </form>
    </div>

    <!-- 부트스트랩 JS 및 Popper.js 추가 -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js"></script>
    <script src="/resources/js/qna.js"></script>
    
    <script>
        // 답변 개수를 저장하는 변수를 추가합니다
        let answerCount = 0;
    
        function showAnswerForm() {
            document.getElementById("answerForm").style.display = "block";
            document.getElementById("answerButton").disabled = true; // 답변하기 버튼 비활성화
        }
    
        function submitAnswer() {
            const answerName = document.getElementById("answerName").value;
            const answerContent = document.getElementById("answerContent").value;
    
            // 답변 내용이 비어있는지 검증합니다
            if (answerContent.trim() === '') {
                alert("답변 내용을 입력하세요.");
                return;
            }
    
            // 이름이 입력되지 않았을 때, 이름을 입력하도록 요청하는 검증합니다
            if (answerName.trim() === '') {
                alert("이름을 입력하세요.");
                return;
            }
    
            const answerSection = document.getElementById("answerSection");
    
            // 답변 개수가 1개 이상이면 추가 등록을 막습니다
            if (answerCount >= 1) {
                alert("답변은 한 개까지만 등록할 수 있습니다.");
                return;
            }
    
            // 답변 내용 생성 및 답변 섹션에 추가
            const answerElement = document.createElement("div");
            answerElement.classList.add("mb-3");
            answerElement.innerHTML = `
                <h2>등록된 답변</h2>
                <div style="padding: 10px 0;"></div>
                <h6>${answerCount === 0 ? '답변 관리자: ' : ''}${answerName}</h6>
                <textarea class="form-control" aria-label="Disabled input example" disabled readonly rows="5">${answerContent}</textarea>
                <div style="padding: 8px 0;"></div>
                <div class="d-grid gap-2 d-md-flex justify-content-md-end mb-3">
                    <button class="btn btn-secondary btn-sm me-2" type="button" onclick="deleteAnswer(this)">삭제</button>
                    <button class="btn btn-secondary btn-sm" type="button" onclick="editAnswer(this)">수정</button>
                </div>
            `;
    
            answerSection.appendChild(answerElement);
    
            // 입력 필드를 초기화하고 답변 작성 폼을 숨깁니다
            document.getElementById("answerName").value = "";
            document.getElementById("answerContent").value = "";
            document.getElementById("answerForm").style.display = "none";
    
            answerCount++;
    
            // 답변이 등록되었으므로 답변하기 버튼을 비활성화합니다
            document.getElementById("answerButton").disabled = true;
        }
    
        function editAnswer(btn) {
            const answerElement = btn.parentElement.parentElement;
            const answerName = answerElement.querySelector("h6").innerText;
    
            // '답변 관리자:' 텍스트를 제거하여 이름만 가져옵니다
            const nameWithoutPrefix = answerName.replace("답변 관리자: ", "");
            const answerContent = answerElement.querySelector("textarea").value;
    
            // 답변 작성 양식에서 수정할 답안의 이름과 내용을 설정합니다
            document.getElementById("answerName").value = nameWithoutPrefix;
            document.getElementById("answerContent").value = answerContent;
    
            // 답변 양식 표시
            document.getElementById("answerForm").style.display = "block";
    
            // 답변을 편집하는 동안 제출 버튼 비활성화
            document.getElementById("answerButton").disabled = true;
    
            // 이전 답변 삭제
            answerElement.remove();
            answerCount--; // 수정한 답변을 삭제했으므로 답변 개수를 감소시킵니다
    
            // 답변이 등록되었으므로 답변하기 버튼을 비활성화합니다
            document.getElementById("answerButton").disabled = true;
            
        }
    
        function deleteAnswer(btn) {
            const answerElement = btn.parentElement.parentElement;
            answerElement.remove();
            answerCount--; // 답변이 삭제되면 답변 개수를 감소시킵니다
    
            // 등록된 답변이 없을 경우에만 답변하기 버튼을 활성화합니다
            if (answerCount === 0) {
                document.getElementById("answerButton").disabled = false;
            }
            
        }
    </script>

</body>

</html>