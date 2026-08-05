<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageStylesheet" value="/resources/css/changePw.css?v=2" />
<%@ include file="/WEB-INF/views/layout/header.jsp" %>
<body class="rs-internal-page account-page d-flex flex-column min-vh-100">
  <%@ include file="/WEB-INF/views/layout/navbar.jsp" %>

  <main class="account-shell">
    <header class="account-heading">
      <p>PASSWORD RESET</p>
      <h1>비밀번호 재설정</h1>
      <span>가입 이메일을 인증한 뒤 새 비밀번호를 입력해 주세요.</span>
    </header>

    <section class="account-card" aria-label="비밀번호 재설정 정보">
      <form class="account-form" onsubmit="return false;">
        <div class="account-field">
          <label for="remail">이메일</label>
          <div class="account-control-row">
            <input class="form-control" type="email" id="remail" placeholder="name@example.com"
                   onkeyup="check_email(event)" autocomplete="email">
            <input type="button" class="btn btn-warning account-row-action" id="emailDulpCheck" value="유저 확인">
          </div>
        </div>

        <div class="account-field">
          <div class="account-label-row">
            <label for="checkInput">이메일 인증번호</label>
            <span id="mail-check-warn" aria-live="polite"></span>
          </div>
          <input class="form-control" type="text" name="checkInput" id="checkInput"
                 placeholder="인증번호 6자리를 입력해 주세요" inputmode="numeric" maxlength="6" required>
        </div>

        <div class="account-field">
          <label for="rpassword">새 비밀번호</label>
          <input class="form-control" type="password" id="rpassword"
                 placeholder="영문, 숫자, 특수문자를 포함한 8~20자"
                 onchange="check_pw()" autocomplete="new-password">
        </div>

        <div class="account-field">
          <div class="account-label-row">
            <label for="rpassword2">새 비밀번호 확인</label>
            <span id="pw_check" aria-live="polite"></span>
          </div>
          <input class="form-control" type="password" id="rpassword2"
                 placeholder="새 비밀번호를 한 번 더 입력해 주세요"
                 onchange="check_pw()" autocomplete="new-password">
        </div>
      </form>

      <form method="POST" name="register_form">
        <input type="hidden" name="pw" id="pw">
        <input type="hidden" name="email" id="email">
      </form>
      <input type="hidden" name="emailok" id="emailok">

      <div class="account-actions">
        <input type="button" class="btn btn-warning" id="changePw" value="비밀번호 변경">
        <input type="button" class="btn btn-outline-dark" id="cancle" value="취소">
      </div>
    </section>
  </main>

  <script src="${CP}/resources/js/login/password-policy.js?v=1"></script>
  <script src="${CP}/resources/js/login/changePw.js?v=2"></script>
  <%@ include file="/WEB-INF/views/layout/footer.jsp" %>
