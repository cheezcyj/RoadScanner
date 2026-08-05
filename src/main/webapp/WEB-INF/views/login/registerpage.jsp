<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageStylesheet" value="/resources/css/membership-style.css?v=2" />
<%@ include file="/WEB-INF/views/layout/header.jsp" %>
<body class="rs-internal-page registration-page d-flex flex-column min-vh-100">
  <%@ include file="/WEB-INF/views/layout/navbar.jsp" %>

  <main class="registration-shell">
    <header class="registration-heading">
      <p>CREATE ACCOUNT</p>
      <h1>RoadScanner 회원가입</h1>
      <span>필수 정보를 입력하고 이메일 인증을 완료해 주세요.</span>
    </header>

    <section class="registration-card" aria-label="회원가입 정보">
      <form action="" method="post" name="membership" class="registration-form">
        <div class="registration-field">
          <label for="id_form">아이디</label>
          <div class="control-row">
            <input type="text" name="id_form" id="id_form"
                   onkeyup="id_form_check(event)" onchange="id_length_check()"
                   placeholder="영문과 숫자를 포함한 6~20자" autocomplete="username">
            <input type="button" class="btn btn-outline-dark control-action" id="idDulpCheck" value="중복 확인">
          </div>
        </div>

        <div class="registration-field">
          <label for="pw_form">비밀번호</label>
          <input type="password" name="pw_form" id="pw_form"
                 placeholder="영문, 숫자, 특수문자를 포함한 8~20자"
                 onchange="check_pw()" autocomplete="new-password">
        </div>

        <div class="registration-field">
          <div class="field-label-row">
            <label for="pw2_form">비밀번호 확인</label>
            <span id="pw_check" aria-live="polite"></span>
          </div>
          <input type="password" name="pw2_form" id="pw2_form"
                 placeholder="비밀번호를 한 번 더 입력해 주세요"
                 onchange="check_pw()" autocomplete="new-password">
        </div>

        <div class="registration-field">
          <label for="email_front">이메일 주소</label>
          <div class="email-row">
            <input type="text" name="email_front" id="email_front"
                   onkeyup="check_email(event)" placeholder="이메일" autocomplete="off">
            <span class="email-at" aria-hidden="true">@</span>
            <input type="text" class="listinput" list="email_list" id="email_back"
                   placeholder="도메인" autocomplete="off">
            <datalist id="email_list">
              <option value="dreamwiz.com">dreamwiz.com</option>
              <option value="empas.com">empas.com</option>
              <option value="freechal.com">freechal.com</option>
              <option value="gmail.com">gmail.com</option>
              <option value="hanmail.net">hanmail.net</option>
              <option value="hanmir.com">hanmir.com</option>
              <option value="hotmail.com">hotmail.com</option>
              <option value="kakao.com">kakao.com</option>
              <option value="korea.com">korea.com</option>
              <option value="lycos.co.kr">lycos.co.kr</option>
              <option value="nate.com">nate.com</option>
              <option value="naver.com">naver.com</option>
              <option value="paran.com">paran.com</option>
              <option value="yahoo.com">yahoo.com</option>
            </datalist>
            <input type="button" class="btn btn-outline-dark control-action" id="emailDulpCheck" value="중복 확인">
          </div>
        </div>

        <div class="registration-field">
          <label for="checkInput">이메일 인증번호</label>
          <div class="control-row verification-row">
            <input type="button" class="btn btn-outline-dark control-action" id="mail-Check-Btn" value="인증번호 전송">
            <input type="text" class="emailcheck" name="checkInput" id="checkInput"
                   placeholder="인증번호 6자리" inputmode="numeric" maxlength="6" required>
          </div>
          <span id="mail-check-warn" class="field-message" aria-live="polite"></span>
        </div>

        <div class="registration-actions">
          <input type="button" class="btn btn-warning" id="register" value="회원가입">
          <input type="button" class="btn btn-outline-dark" id="noneRegister" value="취소">
        </div>
      </form>
    </section>

    <form method="POST" name="register_form">
      <input type="hidden" name="grade" id="grade" value="1">
      <input type="hidden" name="id" id="id">
      <input type="hidden" name="pw" id="pw">
      <input type="hidden" name="email" id="email">
      <input type="hidden" name="auth" id="auth" value="1">
    </form>
    <input type="hidden" name="emailok" id="emailok">
    <input type="hidden" name="idok" id="idok">
  </main>

  <script src="${CP}/resources/js/login/password-policy.js?v=1"></script>
  <script src="${CP}/resources/js/login/register.js?v=2"></script>
  <%@ include file="/WEB-INF/views/layout/footer.jsp" %>
