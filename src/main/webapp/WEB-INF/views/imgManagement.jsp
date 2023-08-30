<header>
	<%@include file="/WEB-INF/views/layout/header.jsp"%>
</header>
<%@page import="com.roadscanner.domain.upload.FileUploadVO"%>
<%@ page contentType="text/html;charset=UTF-8" language="java"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%
	FileUploadVO inVO = (FileUploadVO) request.getAttribute("inVO");
int category = inVO.getCategory();

// 서버 측에서 사용자 세션을 확인하고, 세션이 없으면 기본 페이지로 리다이렉트합니다.
if (session.getAttribute("user") == null) {
	response.sendRedirect("/login");
}
%>
<link rel="stylesheet" href="${CP}/resources/css/imgMng.css">

<%@include file="/WEB-INF/views/layout/navbar.jsp"%>

<body id="font-id">
	<div class="container" style="width: 930px;">
		<!-- 카테고리 선택 -->
		<div class="top-box d-flex justify-content-start">
			<select class="form-select" id="categoryDropdown" name="category" style="width: 200px;">
				<option value="0" <%if (0 == category)out.print("selected");%>>전체</option>
				<option value="10" <%if (10 == category)out.print("selected");%>>기본</option>
				<option value="20" <%if (20 == category)out.print("selected");%>>좋아요</option>
				<option value="30" <%if (30 == category)out.print("selected");%>>싫어요</option>
			</select>
		</div>
		<!-- 카테고리 선택 end -->

		<!-- 전체선택, 삭제버튼, 저장버튼 -->
		<div class="d-flex justify-content-between">
			<div class="form-check" style="margin-top:7px;">
				<input class="form-check-input" type="checkbox" id="selectAllBtn" onclick="toggleSelectAll()"> <label for="form-check-label">전체선택</label>
			</div>
			<div>
				<button type="button" id="selectDeleteBtn" class="btn btn-secondary" onclick="selectDelete()">DELETE</button>
				<button type="button" id="selectSaveBtn" class="btn btn-warning" style="width: 200px; margin-left: 10px; color: white;" onclick="selectSave()">SAVE</button>
			</div>
		</div>
	</div>
	<!-- container end -->

	<!-- 3*3 사진+체크박스 디스플레이 -->
	<div class="table-box d-flex justify-content-center">
		<table>
			<c:choose>
				<c:when test="${not empty list}">
					<c:forEach var="vo" items="${list}" varStatus="status">
						<c:if test="${status.index % 3 == 0}">
							<tr>
						</c:if>
						<td>
							<div class="image-container">
								<div class="checkbox-local">
									<input type="checkbox" class="form-check-input btn_check" id="${vo.name}"> <label for="${vo.name}"></label>
								</div>
								<div class="image-wrapper">
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
    <li class="page-item ${pageNo <= 1 ? 'disabled' : ''}">
        <a class="page-link" href="${CP}/imgManagement?pageNo=1&category=${category}">
            <span>&lt&lt</span>
        </a>
    </li>

    <li class="page-item ${pageNo <= 1 ? 'disabled' : ''} ${prevBlock < 1 ? 'disabled' : ''}">
        <a class="page-link" href="${CP}/imgManagement?pageNo=${prevBlock}&category=${category}">
            <span>&lt</span>
        </a>
    </li>

    <c:forEach begin="${startPage}" end="${endPage}" var="pageNum">
        <li class="page-item ${pageNo == pageNum ? 'active disabled' : ''}">
            <a class="page-link" href="${CP}/imgManagement?pageNo=${pageNum}&category=${category}">
                ${pageNum}
            </a>
        </li>
    </c:forEach>

    <li class="page-item ${nextBlock > totalPages ? 'disabled' : ''}">
        <a class="page-link" href="${CP}/imgManagement?pageNo=${nextBlock}&category=${category}">
            <span>&gt</span>
        </a>
    </li>
    <li class="page-item ${pageNo >= totalPages ? 'disabled' : ''}">
        <a class="page-link" href="${CP}/imgManagement?pageNo=${totalPages}&category=${category}">
            <span>&gt&gt</span>
        </a>
    </li>
  </ul>
	<!-- 페이징 end -->

	<!-- 모달 창 바깥 불투명 검정 배경 -->
	<div class="overlay-modal" id="overlay-modal"></div>

	<!-- 모달 창 -->
	<div class="image-modal" id="imageModal">
		<div class="sort-horizon">
			<div class="left">
				<div class="modalImageWrapper">
					<img src="#" class="modalImage" id="modalImage">
				</div>
				<button type="button" class="btn-close"></button>
			</div>
			<div class="divider"></div>
			<div class="right">
				<table class="table detail_table">
					<tr>
						<th class="text-center fw-bold">번호</th>
						<td id="idx"></td>
					</tr>
					<tr>
						<th class="text-center fw-bold" style="vertical-align: middle;">이름</th>
						<td id="name"></td>
					</tr>
					<tr>
						<th class="text-center fw-bold">업로더</th>
						<td id="id"></td>
					</tr>
					<tr>
						<th class="text-center fw-bold">날짜</th>
						<td id="uploadDate"></td>
					</tr>
					<tr>
						<th class="text-center fw-bold">크기</th>
						<td id="fileSize"></td>
					</tr>
					<tr>
						<th class="text-center fw-bold">상태</th>
						<td id="category"></td>
					</tr>
					<tr>
						<th class="text-center fw-bold">모양 오류</th>
						<td id="u1"></td>
					</tr>
					<tr>
						<th class="text-center fw-bold">색깔 오류</th>
						<td id="u2"></td>
					</tr>
					<tr>
						<th class="text-center fw-bold">그림/숫자 오류</th>
						<td id="u3"></td>
					</tr>
				</table>
				<div style="margin-top: 20px;">
					<button type="button" id="detailDeleteBtn"
						class="btn btn-secondary" style="width: 140px;">DELETE</button>
					<button type="button" id="detailSaveBtn" class="btn btn-warning"
						style="width: 250px; margin-left: 10px; color: white;">SAVE</button>
				</div>
			</div>
		</div>
	</div>
	<!-- 모달 창  end -->
	
  <%-- 세션이 없이 해당 페이지 진입하면 로그인 페이지로 이동 --%>
  <c:if test="${user.grade != 2}">
    <script>     
        window.location.href = "/login";
    </script>   
  </c:if>

	<script src="https://code.jquery.com/jquery-3.7.0.min.js"></script>
	<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
	<script src="${CP}/resources/js/imgMng.js"></script>
</body>
</html>