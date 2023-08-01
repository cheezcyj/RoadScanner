<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib  prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="CP" value="${pageContext.request.contextPath }"/>  
<!DOCTYPE html>
<html>
<head>
<meta charset="${encoding}">
<meta name="viewport" content="width=device-width, initial-scale=1">
<!-- CSS only -->
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-rbsA2VBKQhggwzxH7pPCaAqO46MgnOM80zW1RWuH61DGLwZJEdK2Kadq2F9CUG65" crossorigin="anonymous">
<!-- JavaScript Bundle with Popper -->
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js" integrity="sha384-kenU1KFdBIe4zVF0s0G1M5b4hcpxyD9F7jL+jjXkk+Q2h455rYXK/7HAuoJl+0I4" crossorigin="anonymous"></script>
<script src="${CP}/resources/js/jquery-3.7.0.js"></script>
<script src="${CP}/resources/js/util.js"></script>
<title>로드스캐너 홈페이지</title>
</head>
<body>
<h1>로드스캐너 홈페이지</h1>
    <li><a href="/detail">게시물 상세페이지</a></li>
    <button id="fetchDataButton">Fetch Data</button>
    <div id="dataContainer"></div>

    <script>
        document.getElementById("fetchDataButton").addEventListener("click", fetchData);

        function fetchData() {
            var xhr = new XMLHttpRequest();
            xhr.open("GET", "getData", true);
            xhr.onreadystatechange = function() {
                if (xhr.readyState === XMLHttpRequest.DONE) {
                    if (xhr.status === 200) {
                        var responseData = JSON.parse(xhr.responseText);
                        displayData(responseData);
                    } else {
                        console.error("Failed to fetch data. Status: " + xhr.status);
                    }
                }
            };
            xhr.send();
        }

        function displayData(data) {
            var dataContainer = document.getElementById("dataContainer");
            dataContainer.innerHTML = "<pre>" + JSON.stringify(data, null, 2) + "</pre>";
        }
    </script>
</body>
</html>