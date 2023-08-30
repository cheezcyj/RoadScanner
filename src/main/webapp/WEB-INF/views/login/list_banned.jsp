  <%@include file ="/WEB-INF/views/layout/header.jsp" %>

<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- Bootstrap Icons -->
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.3/font/bootstrap-icons.css"> 
<!--  JS  -->
<script src="https://code.jquery.com/jquery-3.7.0.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js"></script>
<style>
/* 부트스트랩 호버 수정 */
.form-control:focus {
  box-shadow: none;
  border-color: #666666;
}
.page-link {
  color: #343a40;
}
.page-item.active .page-link {
  z-index: 1;
  color: white;
  border-color: #6c757d;
  background-color: #6c757d;
}
.page-link:focus, .page-link:hover {
  color: #343a40;
  box-shadow: none;
}
</style>
<title>회원 관리</title>

<body id="font-id">
<!-- 정지회원 리스트  ---------------------------------------------------------------> 
<div class="container"> 
    <form>  
        <!-- 제목 -->
        <div class="page-header">
            <h2 class="text-center">정지회원 리스트</h2>
        </div>
        <!-- 제목 end --------------------------------------------------------------->

        <!-- 회원 정보 테이블 -->
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
                <c:forEach var="list" items="${list}">
                 <c:set var="i" value="${i+1}"></c:set>
                 <c:set var="j" value="${(select-1)*5+i}"></c:set>  
                  <c:if test="${user.id ne list.id}">
	                    <tr>
	                        <td><input type="checkbox" name="delcheckbox" value ="${list.id}"></td>
	                        <td class="text-center col-sm-1">${j}</td>
	                        <td class="text-center col-sm-5">${list.id}</td>
	                        <td class="text-center col-sm-6">${list.email}</td>
	                    </tr>
	                </c:if>
                </c:forEach>   
            </tbody>
        </table>
        <input type="hidden" id="messagebox">
        <!-- 회원 정보 테이블 end ------------------------------------------------------------>
        
        <!-- 검색 폼 -->
        <div class="row mb-3">
            <div class="col">
                    <div class="form-group" style="display: flex;">
                        <input type="text" id ="searchid" name="keyword" class="form-control" value="<c:out value='${BannedPage.keyword}'/>" placeholder="아이디 검색">
                     <div style="margin-left : 7px;">
	                    <button type="submit" id ="searchidbtn" class="btn btn-secondary ml-2"
	                    style="width: 70px;">검색</button>
	                 </div>
	                 <div style="margin-left : 7px;">    	                    
	                    <button type="button" id= "deletebtn" class="btn btn-warning ml-2"
	                    style="width: 120px; color: white;">정지해제</button>
                    </div>
                    </div>
            </div>
        </div>
        <!-- 검색 폼 end ------------------------------------------------------------>

        <!-- pagination -->
        <nav aria-label="Page navigation">
            <ul class="pagination justify-content-center">
                <c:if test="${BannedPage.prev}">  
				    <li class="page-item"><a class="page-link" href="/login/list_banned?num=1&keyword=${BannedPage.keyword}"> << </a></li>
				</c:if>
                   
                <c:if test="${BannedPage.prev}">
                    <li class="page-item"><a class="page-link" aria-label="Previous" href="/login/list_banned?num=${BannedPage.startPagenum - 5}&keyword=${BannedPage.keyword}">이전</a></li>
                </c:if>
                
                <c:forEach begin="${BannedPage.startPagenum}" end="${BannedPage.endPagenum}" var="num">
                        <li class="page-item ${select == num  ? 'active' : ''} ${select == num  ? 'disabled' : ''}">
                        <a class="page-link" href="/login/list_banned?num=${num}&keyword=${BannedPage.keyword}">${num}</a></li>
                </c:forEach>
                
                <c:if test="${BannedPage.next}">  
                    <li class="page-item"><a class="page-link" href="/login/list_banned?num=${BannedPage.endPagenum + 1}&keyword=${BannedPage.keyword}">다음</a></li>
                </c:if>
                
                <c:if test="${BannedPage.next}">  
				    <li class="page-item"><a class="page-link" href="/login/list_banned?num=${Math.round((Math.ceil(BannedPage.count/BannedPage.pageNumCnt)))}&keyword=${BannedPage.keyword}">>></a></li>
				</c:if>
                
            </ul>
        </nav>
<!-- pagination end ------------------------------------------------------->
     </form> 
</div>     
<!-- 정지회원 리스트 end --------------------------------------------------------------->    
<!-- container end --------------------------------------------------------------->    

</body>
<script>
$("#searchidbt").on("click",function(){
	let keyword3 = $("#searchid").value();
	console.log(keyword);
	
	location.href = "/login/list_banned?num=1"+ "&keyword=" + keyword;
});
</script>

<script>
$("#deletebtn").on("click",function(){
  
    
    $("input[name='delcheckbox']").each(function(){
        if( $(this).is(":checked") == true ){
          var tmpVal = $(this).val();

              // AJAX 요청을 보냅니다.
                  $.ajax({
                      type: "POST",
                      url:"${CP}/clear",
                      dataType:"html",
                      data: {
                       id: tmpVal
                      },
                      success:function(data) {
                       let parsedJSON = JSON.parse(data);
                         
                           if("10" == parsedJSON.msgId){
                              $('#messagebox3').attr('value', parsedJSON.msgContents);
                              window.location.href= "/login/list_banned?num=1&keyword=${BannedPage.keyword}";
                          } 
                                                
                          if("20" == parsedJSON.msgId){
                              $('#messagebox3').attr('value',parsedJSON.msgContents);
                              return;  
                          }
                      },
                      error: function(data) {
                          console.log("error:" + data);
                  }
              }); // --ajax
           
        }
      }); //--체크박스 체크
    $('#member_iframe', parent.document).get(0).contentDocument.location.reload(); 
    location.reload();
});
</script>

</html>