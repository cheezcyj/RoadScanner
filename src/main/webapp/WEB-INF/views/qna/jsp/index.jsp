<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>로드스캐너</title>
</head>
<body>
    <h1>Q&A 게시판</h1>
    <div>
        <div>
            <a href="/qna/save" role="button">글쓰기</a>
        </div>
    </div>
    <br>
    <%-- 이곳에 글목록이 나와요. 목록 출력영역 <--%>
    <table>
        <thead>
        <tr>
            <th>번호</th>
            <th>분류</th>
            <th>제목</th>
            <th>작성자</th>
            <th>작성일</th>
            <th>조회수</th>
        </tr>
        </thead>
        <tbody>
        <%--    model을 통해 받은 데이터를 반복해서 출력 --%>
        <c:forEach items="${questions}" var="question">
            <tr>
                <td>${question.no}</td>
                <td>${question.category}</td>
                <td><a href="/qna/${question.no}">${question.title}</a></td>
                <td>${question.id}</td>
                <td>${question.createDate}</td>
                <td>${question.views}</td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</body>
</html>
