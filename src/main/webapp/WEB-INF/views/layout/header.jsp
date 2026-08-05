<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="CP" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#102a43">
    <title>RoadScanner</title>
    <link rel="icon" type="image/svg+xml" href="${CP}/resources/img/favicon.svg">
    <meta name="csrf-token" content="${csrfToken}">
    <meta name="csrf-header" content="${csrfHeaderName}">
    <meta name="csrf-parameter" content="${csrfParameterName}">
    <meta name="application-context" content="${pageContext.request.contextPath}">
    <link href="${CP}/resources/css/bootstrap/bootstrap.min.css" rel="stylesheet">
    <link href="${CP}/resources/css/common.css?v=13" rel="stylesheet">
    <c:if test="${not empty pageStylesheet}">
        <link href="${CP}${pageStylesheet}" rel="stylesheet">
    </c:if>
    <script src="${CP}/resources/js/jquery-3.7.0.js"></script>
    <script src="${CP}/resources/js/bootstrap/bootstrap.bundle.min.js" defer></script>
    <script src="${CP}/resources/js/csrf.js?v=2" defer></script>
</head>
