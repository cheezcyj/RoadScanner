<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageStylesheet" value="/resources/css/admin.css?v=3" />
<%@ include file="/WEB-INF/views/layout/header.jsp" %>
<body class="rs-internal-page admin-page d-flex flex-column min-vh-100">
  <%@ include file="/WEB-INF/views/layout/navbar.jsp" %>

  <main class="admin-container">
    <header class="admin-heading">
      <div class="admin-heading-copy">
        <p>ACCOUNT MANAGEMENT</p>
        <h1>Account List</h1>
        <span>계정 유형별 목록을 확인하고 상태를 안전하게 관리합니다.</span>
      </div>
      <nav class="admin-section-nav" aria-label="계정 목록 바로가기">
        <a href="#member-panel"><b>01</b> 일반 회원</a>
        <a href="#admin-panel"><b>02</b> 관리자</a>
        <a href="#banned-panel"><b>03</b> 이용 정지</a>
      </nav>
    </header>

    <section class="admin-panel" id="member-panel" aria-labelledby="member-panel-title">
      <header class="admin-panel-header">
        <span class="admin-panel-index" aria-hidden="true">01</span>
        <div>
          <h2 id="member-panel-title">일반 회원</h2>
          <p>일반 회원을 검색하거나 선택한 계정의 이용을 정지할 수 있습니다.</p>
        </div>
      </header>
      <iframe id="member_iframe" class="admin-list-frame" title="일반 회원 목록"
          src="${CP}/login/list_member" loading="lazy"></iframe>
    </section>

    <section class="admin-panel" id="admin-panel" aria-labelledby="admin-panel-title">
      <header class="admin-panel-header">
        <span class="admin-panel-index" aria-hidden="true">02</span>
        <div>
          <h2 id="admin-panel-title">관리자</h2>
          <p>현재 관리자를 제외한 관리자 계정을 조회하고 관리합니다.</p>
        </div>
      </header>
      <iframe id="admin_iframe" class="admin-list-frame" title="관리자 목록"
          src="${CP}/login/list_admin" loading="lazy"></iframe>
    </section>

    <section class="admin-panel" id="banned-panel" aria-labelledby="banned-panel-title">
      <header class="admin-panel-header">
        <span class="admin-panel-index" aria-hidden="true">03</span>
        <div>
          <h2 id="banned-panel-title">이용 정지 회원</h2>
          <p>이용이 정지된 계정을 확인하고 필요한 경우 정지를 해제합니다.</p>
        </div>
      </header>
      <iframe id="banned_iframe" class="admin-list-frame" title="이용 정지 회원 목록"
          src="${CP}/login/list_banned" loading="lazy"></iframe>
    </section>
  </main>

  <script src="${CP}/resources/js/admin-page.js?v=1"></script>
  <%@ include file="/WEB-INF/views/layout/footer.jsp" %>
