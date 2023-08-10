<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="CP" value="${pageContext.request.contextPath }"/>

<%
 String strReferer = request.getHeader("referer");
 if(strReferer == null){
%>
 <script language="javascript">
  alert("접속을 차단합니다.");
  document.location.href="${CP}/login";
 </script>
<%
 return;
 }
%>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <!-- Bootstrap Icons -->
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.3/font/bootstrap-icons.css">
    <!-- CSS only -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-rbsA2VBKQhggwzxH7pPCaAqO46MgnOM80zW1RWuH61DGLwZJEdK2Kadq2F9CUG65" crossorigin="anonymous">
    <!-- JavaScript Bundle with Popper -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js" integrity="sha384-kenU1KFdBIe4zVF0s0G1M5b4hcpxyD9F7jL+jjXkk+Q2h455rYXK/7HAuoJl+0I4" crossorigin="anonymous"></script>
    <script src="${CP}/resources/js/jquery-3.7.0.js"></script>
    <title>회원 관리</title>
</head>
<body>
<!-- 정지회원 리스트  ---------------------------------------------------------------> 
<div class="container"> 
    <form>  
        <!-- 제목 -->
        <div class="page-header">
            <h2 class="text-center">관리자 리스트</h2>
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
                <c:forEach var="list2" items="${list2}">
                 <c:set var="i" value="${i+1}"></c:set>
                 <c:set var="j" value="${(select2-1)*5+i}"></c:set>  
                  <c:if test="${user.id ne list2.id}">
	                    <tr>
	                        <td><input type="checkbox" name="delcheckbox2" value ="${list2.id}"></td>
	                        <td class="text-center col-sm-1">${j}</td>
	                        <td class="text-center col-sm-5">${list2.id}</td>
	                        <td class="text-center col-sm-6">${list2.email}</td>
	                    </tr>
	                </c:if>
                </c:forEach>   
            </tbody>
        </table>
        <input type="hidden" id="messagebox2">
        <!-- 회원 정보 테이블 end ------------------------------------------------------------>
        
        <!-- 검색 폼 -->
        <div class="row mb-3">
            <div class="col">
                    <div class="form-group">
                        <input type="text" id ="searchid3" name="keyword2" class="form-control" placeholder="아이디 검색">
                    </div>
                    <button type="submit" id ="searchidbtn3" class="btn btn-primary ml-2">검색</button>
                    <button type="button" id= "deletebtn3" class="btn btn-primary ml-2">미정</button>
            </div>
        </div>
        <!-- 검색 폼 end ------------------------------------------------------------>

        <!-- pagination -->
        <nav aria-label="Page navigation">
            <ul class="pagination justify-content-center">
                <c:if test="${page3.prev3}">
                    <li class="page-item"><a class="page-link" aria-label="Previous" href="/memberAdmin2?num2=${page2.startPageNum2 - 5}&keyword=${page.keyword}">이전</a></li>
                </c:if>
                
                <c:forEach begin="${page3.startPageNum3}" end="${page3.endPageNum3}" var="num2">      
                      <c:if test="${select != num3}">
                        <li class="page-item"><a class="page-link" href="/memberAdmin3?num3=${num3}&keyword=${page.keyword}">${num2}</a></li>
                      </c:if>
                      
                      <c:if test="${select == num3}">
                        <li class="page-item"><a class="page-link" href="/memberAdmin3?num3=${num3}&keyword=${page.keyword}">${num2}</a></li>
                      </c:if>
                </c:forEach>
                
                <c:if test="${page3.next3}">  
                    <li class="page-item"><a class="page-link" href="/memberAdmin2?num2=${page3.endPageNum3 + 1}&keyword=${page.keyword}">다음</a></li>
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
$("#searchidbt3").on("click",function(){
	let keyword2 = $("#searchid3").value();
	console.log(keyword2);
	
	location.href = "/memberAdmin3?num=1"+ "&keyword3=" + keyword2;
});
</script>

<script>
$("#deletebtn3").on("click",function(){
    console.log("haha");
    
    $("input[name='delcheckbox3']").each(function(){
        if( $(this).is(":checked") == true ){
          var tmpVal2 = $(this).val();
          console.log(tmpVal2);
        
          
          
              // AJAX 요청을 보냅니다.
                  $.ajax({
                      type: "POST",
                      url:"${CP}/withdraw",
                      dataType:"html",
                      data: {
                       id: tmpVal2
                      },
                      success:function(data) {
                       let parsedJSON = JSON.parse(data);
                         
                           if("10" == parsedJSON.msgId){
                              $('#messagebox').attr('value', parsedJSON.msgContents);
                              location.reload();
                          } 
                                                
                          if("20" == parsedJSON.msgId){
                              $('#messagebox').attr('value',parsedJSON.msgContents);
                              location.reload();
                          }
                      },
                      error: function(data) {
                          console.log("error:" + data);
                  }
              }); // --ajax
           
        }
      }); //--체크박스 체크
    location.reload();
});
</script>
</html>