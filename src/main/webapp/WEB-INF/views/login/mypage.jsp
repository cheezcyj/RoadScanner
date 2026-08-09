<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageStylesheet" value="/resources/css/mypage.css?v=3" />
<%@ include file="/WEB-INF/views/layout/header.jsp" %>
<body class="rs-internal-page profile-page d-flex flex-column min-vh-100">
  <%@ include file="/WEB-INF/views/layout/navbar.jsp" %>

  <c:if test="${user ne null}">
    <main class="profile-shell">
      <header class="profile-heading">
        <p>MY ACCOUNT</p>
        <c:choose>
          <c:when test="${user.grade == 2}">
            <h1>관리자 마이페이지</h1>
          </c:when>
          <c:otherwise>
            <h1>마이페이지</h1>
          </c:otherwise>
        </c:choose>
        <span>계정 정보와 비밀번호를 안전하게 관리하세요.</span>
      </header>

      <section class="profile-card" aria-label="계정 정보">
        <form class="profile-form" onsubmit="return false;">
          <div class="profile-field">
            <label for="rid">아이디</label>
            <input class="form-control profile-readonly" type="text" id="rid" readonly value="<c:out value='${user.id}'/>">
          </div>
          <div class="profile-field">
            <label for="currentPassword">현재 비밀번호</label>
            <input class="form-control" type="password" id="currentPassword"
                   placeholder="현재 비밀번호를 입력해 주세요"
                   autocomplete="current-password">
          </div>
          <div class="profile-field">
            <label for="rpassword">새 비밀번호</label>
            <input class="form-control" type="password" id="rpassword"
                   placeholder="영문, 숫자, 특수문자를 포함한 8~20자"
                   onchange="check_pw()" autocomplete="new-password">
          </div>
          <div class="profile-field">
            <div class="profile-label-row">
              <label for="rpassword2">새 비밀번호 확인</label>
              <span id="pw_check" aria-live="polite"></span>
            </div>
            <input class="form-control" type="password" id="rpassword2"
                   placeholder="새 비밀번호를 한 번 더 입력해 주세요"
                   onchange="check_pw()" autocomplete="new-password">
          </div>
          <div class="profile-field">
            <label for="remail">이메일</label>
            <input class="form-control profile-readonly" type="text" id="remail" readonly value="<c:out value='${user.email}'/>">
          </div>
        </form>

        <div class="profile-actions">
          <input type="button" class="btn btn-warning" id="update" value="정보 수정">
          <input type="button" class="btn btn-outline-dark" id="cancle" value="취소">
        </div>

        <c:if test="${user.grade == 1}">
          <div class="profile-links">
            <input type="button" class="btn btn-outline-dark" id="myQnAboard" value="문의하기">
            <input type="button" class="btn profile-danger-button" id="withdraw" value="회원 탈퇴">
          </div>
        </c:if>
      </section>
    </main>
  </c:if>

  <script src="${CP}/resources/js/login/password-policy.js?v=1"></script>
  <script src="${CP}/resources/js/login/mypage.js?v=3"></script>
  <%@ include file="/WEB-INF/views/layout/footer.jsp" %>
