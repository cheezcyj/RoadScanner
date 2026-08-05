<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageStylesheet" value="/resources/css/admin.css?v=3" />
<%@include file ="/WEB-INF/views/layout/header.jsp" %>
<c:url var="bannedListUrl" value="/login/list_banned" />

<body class="admin-list-page">
<!-- 정지회원 리스트  ---------------------------------------------------------------> 
<main class="container admin-list-shell"> 
    <form class="admin-list-form" method="get" action="<c:out value='${bannedListUrl}'/>">
        <input type="hidden" name="num" value="1">
        <h1 class="visually-hidden">이용 정지 회원 목록</h1>

        <!-- 회원 정보 테이블 -->
        <div class="admin-table-wrap">
        <table class="table table-hover" id="memberTable">
            <thead class="table-light">
                <tr>
                    <th class="text-center"></th>
                    <th class="text-center">NO</th>
                    <th class="text-center">아이디</th>
                    <th class="text-center">이메일</th>
                </tr>
            </thead>
            <tbody>   
                <c:forEach var="list" items="${list}" varStatus="status">
	                    <tr>
	                        <td><input type="checkbox" name="delcheckbox" value ="<c:out value='${list.id}'/>"></td>
	                        <td class="text-center col-sm-1">${(bannedPage.page - 1) * bannedPage.pageSize + status.count}</td>
	                        <td class="text-center col-sm-5"><c:out value="${list.id}"/></td>
	                        <td class="text-center col-sm-6"><c:out value="${list.email}"/></td>
	                    </tr>
                </c:forEach>   
                <c:if test="${empty list}">
                    <tr><td class="admin-empty-state" colspan="4">이용이 정지된 회원이 없습니다.</td></tr>
                </c:if>
            </tbody>
        </table>
        </div>
        <input type="hidden" id="messagebox">
        <!-- 회원 정보 테이블 end ------------------------------------------------------------>
        
        <!-- 검색 폼 -->
        <div class="row mb-3 admin-list-toolbar-row">
            <div class="col">
                    <div class="form-group admin-list-toolbar">
                        <input type="text" id ="searchid" name="keyword" class="form-control" value="<c:out value='${bannedPage.keyword}'/>" placeholder="아이디 검색">
	                    <button type="submit" id="searchidbtn" class="btn btn-secondary">검색</button>
	                    <button type="button" id="deletebtn" class="btn btn-warning"
	                    data-account-action data-endpoint="${CP}/clear" data-refresh-frame="member_iframe"
	                    >정지 해제</button>
                    </div>
            </div>
        </div>
        <!-- 검색 폼 end ------------------------------------------------------------>

        <!-- pagination -->
        <nav class="admin-list-pagination" aria-label="이용 정지 회원 페이지 이동">
            <ul class="pagination justify-content-center">
                <c:if test="${bannedPage.previous}">  
				    <c:url var="bannedFirstUrl" value="/login/list_banned">
				        <c:param name="num" value="1"/>
				        <c:param name="keyword" value="${bannedPage.keyword}"/>
				    </c:url>
				    <li class="page-item"><a class="page-link" href="<c:out value='${bannedFirstUrl}'/>"> << </a></li>
				</c:if>
                   
                <c:if test="${bannedPage.previous}">
                    <c:url var="bannedPreviousUrl" value="/login/list_banned">
                        <c:param name="num" value="${bannedPage.previousPage}"/>
                        <c:param name="keyword" value="${bannedPage.keyword}"/>
                    </c:url>
                    <li class="page-item"><a class="page-link" aria-label="Previous" href="<c:out value='${bannedPreviousUrl}'/>">이전</a></li>
                </c:if>
                
                <c:forEach begin="${bannedPage.startPage}" end="${bannedPage.endPage}" var="num">
                        <c:url var="bannedPageUrl" value="/login/list_banned">
                            <c:param name="num" value="${num}"/>
                            <c:param name="keyword" value="${bannedPage.keyword}"/>
                        </c:url>
                        <li class="page-item ${bannedPage.page == num ? 'active disabled' : ''}">
                        <a class="page-link" href="<c:out value='${bannedPageUrl}'/>">${num}</a></li>
                </c:forEach>
                
                <c:if test="${bannedPage.next}">  
                    <c:url var="bannedNextUrl" value="/login/list_banned">
                        <c:param name="num" value="${bannedPage.nextPage}"/>
                        <c:param name="keyword" value="${bannedPage.keyword}"/>
                    </c:url>
                    <li class="page-item"><a class="page-link" href="<c:out value='${bannedNextUrl}'/>">다음</a></li>
                </c:if>
                
                <c:if test="${bannedPage.next}">  
				    <c:url var="bannedLastUrl" value="/login/list_banned">
				        <c:param name="num" value="${bannedPage.totalPages}"/>
				        <c:param name="keyword" value="${bannedPage.keyword}"/>
				    </c:url>
				    <li class="page-item"><a class="page-link" href="<c:out value='${bannedLastUrl}'/>">>></a></li>
				</c:if>
                
            </ul>
        </nav>
<!-- pagination end ------------------------------------------------------->
        <p id="accountActionMessage" class="admin-list-message text-center" role="status" aria-live="polite"></p>
     </form> 
</main>     
<!-- 정지회원 리스트 end --------------------------------------------------------------->    
<!-- container end --------------------------------------------------------------->    

<script src="${CP}/resources/js/admin-member-actions.js"></script>
</body>
</html>
