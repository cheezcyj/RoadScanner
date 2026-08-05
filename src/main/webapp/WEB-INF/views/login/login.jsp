<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageStylesheet" value="/resources/css/default.css?v=2" />
<%@ include file="/WEB-INF/views/layout/header.jsp" %>

<body class="auth-page d-flex flex-column min-vh-100">
  <c:choose>
    <c:when test="${user eq null}">
      <main class="auth-shell">
        <a href="${CP}/main" id="head-logo" aria-label="RoadScanner 메인으로 이동">RoadScanner</a>

        <section class="auth-card" aria-labelledby="login-title">
          <div class="auth-heading">
            <p>WELCOME BACK</p>
            <h1 id="login-title">로그인</h1>
            <span>이미지 분석과 게시판 기능을 계속 이용해 보세요.</span>
          </div>

          <form class="loginbox" action="${CP}/login" method="post" onsubmit="return false;">
            <input type="hidden" name="${csrfParameterName}" value="${csrfToken}" />
            <label class="loginboxdiv" for="id">
              <img class="icon" src="${CP}/resources/img/usericon.png" alt="" />
              <input class="loginidpwbtn" type="text" id="id" name="id"
                     placeholder="아이디" autocomplete="username" />
            </label>

            <label class="loginboxdiv" for="pw">
              <img class="icon" src="${CP}/resources/img/passwordicon.png" alt="" />
              <input class="loginidpwbtn" type="password" id="pw" name="pw"
                     placeholder="비밀번호" autocomplete="current-password" />
            </label>

            <button type="submit" class="btn btn-warning loginbtn" id="doLogin" name="doLogin">로그인</button>

            <div id="button-div" aria-label="계정 도움말">
              <button type="button" class="btn btn-outline-dark for-btn-center"
                      onclick="window.location.href='${CP}/findIdPw';">ID/PW 찾기</button>
              <button type="button" class="btn btn-outline-dark for-btn-center"
                      onclick="window.location.href='${CP}/registerpage';">회원가입</button>
            </div>
          </form>
        </section>
      </main>
    </c:when>
    <c:otherwise>
      <main class="auth-shell">
        <section class="auth-card auth-state-card">
          <h1>현재 로그인 상태입니다.</h1>
          <p>로그아웃한 뒤 다른 계정으로 로그인할 수 있습니다.</p>
          <img src="${CP}/resources/img/infinite.gif" alt="처리 중" />
          <button type="button" class="btn btn-outline-dark" onclick="window.roadscannerLogout()">로그아웃</button>
        </section>
      </main>
    </c:otherwise>
  </c:choose>

  <script src="${CP}/resources/js/login/login.js?v=4"></script>
  <%@ include file="/WEB-INF/views/layout/footer.jsp" %>
