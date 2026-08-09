<%@ page contentType="text/html;charset=UTF-8" language="java"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<c:set var="pageStylesheet" value="/resources/css/imgMng.css?v=3" />
<%@include file="/WEB-INF/views/layout/header.jsp"%>
<body class="rs-internal-page img-management-page">
	<%@include file="/WEB-INF/views/layout/navbar.jsp"%>
	<main class="img-management-main">
	<div class="container img-management-toolbar">
		<!-- 카테고리 선택 -->
		<div class="top-box d-flex justify-content-start">
			<select class="form-select" id="categoryDropdown" name="category">
				<option value="0" ${category == 0 ? 'selected' : ''}>전체</option>
				<option value="10" ${category == 10 ? 'selected' : ''}>기본</option>
				<option value="20" ${category == 20 ? 'selected' : ''}>좋아요</option>
				<option value="30" ${category == 30 ? 'selected' : ''}>싫어요</option>
			</select>
		</div>
		<!-- 카테고리 선택 end -->

		<!-- 전체선택, 삭제버튼, 저장버튼 -->
		<div class="d-flex justify-content-between">
			<div class="form-check img-select-all">
				<input class="form-check-input" type="checkbox" id="selectAllBtn" onclick="toggleSelectAll()"> <label for="selectAllBtn">전체선택</label>
			</div>
			<div class="img-bulk-actions">
				<button type="button" id="selectDeleteBtn" class="btn btn-secondary" onclick="selectDelete()">DELETE</button>
				<button type="button" id="selectSaveBtn" class="btn btn-warning" onclick="selectSave()">SAVE</button>
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
									<input type="checkbox" class="form-check-input btn_check" id="upload-${vo.idx}-${status.index}" data-name="<c:out value='${vo.name}'/>"> <label for="upload-${vo.idx}-${status.index}"></label>
								</div>
								<div class="image-wrapper">
									<img class="uploaded-image" src="<c:out value='${vo.url}'/>" alt="<c:out value='${vo.name}'/>">
								</div>
							</div> <!-- image-container end -->
						</td>
						<c:if test="${(status.index + 1) % 3 == 0 || status.last}">
							</tr>
						</c:if>
					</c:forEach>
				</c:when>
				<c:otherwise>
					<tr><td rowspan="3" colspan="3">No data found</td></tr>
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
	</main>

	<!-- 모달 창 바깥 불투명 검정 배경 -->
	<div class="overlay-modal" id="overlay-modal"></div>

	<!-- 모달 창 -->
	<div class="image-modal" id="imageModal">
		<div class="sort-horizon">
			<div class="left">
				<div class="modalImageWrapper">
					<img class="modalImage" id="modalImage" alt="선택한 이미지 미리보기">
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
				<div class="img-detail-actions">
					<button type="button" id="detailDeleteBtn"
						class="btn btn-secondary">DELETE</button>
					<button type="button" id="detailSaveBtn" class="btn btn-warning"
						>SAVE</button>
				</div>
			</div>
		</div>
	</div>
	<!-- 모달 창  end -->
	
	<script src="${CP}/resources/js/imgMng.js"></script>
  <%@include file ="/WEB-INF/views/layout/footer.jsp" %>
