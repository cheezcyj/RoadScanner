<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageStylesheet" value="/resources/css/default.css?v=2" />
<%@ include file="/WEB-INF/views/layout/header.jsp" %>
<body class="rs-internal-page recovery-page d-flex flex-column min-vh-100">
  <%@ include file="/WEB-INF/views/layout/navbar.jsp" %>
  <script>
    history.replaceState({}, null, location.pathname);
  </script>

  <main class="recovery-shell">
    <header class="recovery-heading">
      <p>ACCOUNT RECOVERY</p>
      <h1>계정 정보를 잊으셨나요?</h1>
      <span>가입할 때 사용한 정보로 아이디를 찾거나 비밀번호를 재설정할 수 있습니다.</span>
    </header>

    <div class="recovery-grid">
      <section class="recovery-card" aria-labelledby="find-id-title">
        <div class="recovery-card-heading">
          <span aria-hidden="true">01</span>
          <div>
            <h2 id="find-id-title">아이디 찾기</h2>
            <p>가입 이메일을 입력해 주세요.</p>
          </div>
        </div>
        <form class="recovery-form" onsubmit="return false;">
          <label for="email">이메일</label>
          <input type="email" class="findinput" id="email" name="email"
                 placeholder="name@example.com" autocomplete="email">
          <input type="hidden" id="set_id">
          <button type="button" class="btn btn-secondary findbtn" id="findId" name="findId">아이디 찾기</button>
        </form>
        <input type="hidden" id="id" name="id">
      </section>

      <section class="recovery-card" aria-labelledby="find-password-title">
        <div class="recovery-card-heading">
          <span aria-hidden="true">02</span>
          <div>
            <h2 id="find-password-title">비밀번호 재설정</h2>
            <p>아이디와 가입 이메일을 확인합니다.</p>
          </div>
        </div>
        <form class="recovery-form" onsubmit="return false;">
          <label for="userId">아이디</label>
          <input type="text" class="findinput" id="userId" name="userId"
                 onkeyup="id_form_check(event)" placeholder="아이디" autocomplete="username">
          <label for="email2">이메일</label>
          <input type="email" class="findinput" id="email2" name="email2"
                 placeholder="name@example.com" autocomplete="email">
          <button type="button" class="btn btn-secondary findbtn" id="findPw" name="findPw">비밀번호 재설정</button>
        </form>
      </section>
    </div>
  </main>

  <script src="${CP}/resources/js/login/findIdAndPw.js"></script>
  <%@ include file="/WEB-INF/views/layout/footer.jsp" %>
