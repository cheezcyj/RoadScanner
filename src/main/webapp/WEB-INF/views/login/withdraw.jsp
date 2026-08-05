<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageStylesheet" value="/resources/css/withdraw.css?v=2" />
<%@ include file="/WEB-INF/views/layout/header.jsp" %>
<body class="rs-internal-page withdraw-page d-flex flex-column min-vh-100">
  <%@ include file="/WEB-INF/views/layout/navbar.jsp" %>

  <c:choose>
    <c:when test="${user ne null}">
      <main class="withdraw-shell">
        <section class="withdraw-card" aria-labelledby="withdraw-title">
          <div class="withdraw-icon" aria-hidden="true">!</div>
          <p class="withdraw-eyebrow">ACCOUNT WITHDRAWAL</p>
          <h1 id="withdraw-title">회원 탈퇴</h1>
          <p class="withdraw-description">
            탈퇴하면 계정 이용이 중지됩니다. 계속하려면 현재 비밀번호를 입력해 주세요.
          </p>

          <form class="withdraw-form" onsubmit="return false;">
            <label for="rawPassword">현재 비밀번호</label>
            <input type="password" id="rawPassword" name="rawPassword"
                   placeholder="비밀번호를 입력하세요" autocomplete="current-password">
            <input type="hidden" id="id" name="id" value="<c:out value='${user.id}'/>">
            <input type="button" class="btn withdraw-submit" id="withdraw" value="회원 탈퇴하기">
          </form>
        </section>
      </main>
    </c:when>
    <c:otherwise>
      <main class="withdraw-shell">
        <section class="withdraw-card withdraw-login-state">
          <h1>로그인이 필요합니다.</h1>
          <p>회원 탈퇴는 로그인 후 진행할 수 있습니다.</p>
          <a class="btn btn-outline-dark" href="${CP}/login">로그인으로 이동</a>
        </section>
      </main>
    </c:otherwise>
  </c:choose>

  <script src="${CP}/resources/js/login/withdraw.js?v=2"></script>
  <%@ include file="/WEB-INF/views/layout/footer.jsp" %>
