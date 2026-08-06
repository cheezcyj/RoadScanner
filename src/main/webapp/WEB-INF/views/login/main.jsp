<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="CP" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="theme-color" content="#07111f">
  <meta name="description" content="교통표지판 후보를 찾아 독일 GTSRB 43종 범위에서 분류하는 RoadScanner 서비스">
  <link rel="icon" type="image/svg+xml" href="${CP}/resources/img/favicon.svg">
  <link rel="preload" as="image" type="image/webp"
      href="${CP}/resources/picture/driving-hero-poster.webp?v=1" fetchpriority="high">
  <link rel="stylesheet" href="${CP}/resources/css/main.css?v=14">
  <script src="${CP}/resources/js/main.js?v=1" defer></script>
  <title>RoadScanner</title>
</head>
<body>
  <a class="skip-link" href="#main-content">본문 바로가기</a>

  <header class="site-header">
    <a class="brand" href="#intro" aria-label="RoadScanner 인트로로 이동">
      <span class="brand-mark" aria-hidden="true"></span>
      <span>RoadScanner</span>
    </a>

    <nav class="site-nav" aria-label="주요 메뉴">
      <a href="#home">서비스 소개</a>
    </nav>

    <c:choose>
      <c:when test="${empty user}">
        <a class="header-action" href="${CP}/login">로그인</a>
      </c:when>
      <c:otherwise>
        <a class="header-action" href="${CP}/main/preUpload">사진 분석</a>
      </c:otherwise>
    </c:choose>
  </header>

  <main id="main-content">
    <section class="story-section intro-section" id="intro" aria-labelledby="intro-title">
      <picture class="intro-media" aria-hidden="true">
        <source srcset="${CP}/resources/video/driving-hero.webp?v=1" type="image/webp">
        <img class="background-gif intro-gif" src="${CP}/resources/video/driving.gif"
            alt="" width="1280" height="720" loading="eager" decoding="async">
      </picture>
      <div class="media-scrim intro-scrim" aria-hidden="true"></div>

      <div class="section-content intro-content">
        <p class="intro-overline">AI ROAD SIGN RECOGNITION</p>
        <h1 class="intro-logo" id="intro-title">RoadScanner</h1>
        <div class="intro-signature" aria-hidden="true">
          <span></span>
          <b>VISION IN MOTION</b>
          <span></span>
        </div>
        <p class="intro-description">도로 위 장면에서 교통표지판을 찾아 더 빠르고 명확하게.</p>
      </div>

      <a class="section-scroll-link" href="#home" aria-label="아래로 내려가 서비스 화면 보기">
        <span class="scroll-chevrons" aria-hidden="true"><i></i><i></i></span>
      </a>
    </section>

    <section class="story-section hero-section" id="home" aria-labelledby="hero-title">
      <div class="media-scrim hero-scrim" aria-hidden="true"></div>

      <div class="section-content hero-content">
        <p class="eyebrow"><span></span> AI ROAD SIGN RECOGNITION</p>
        <h2 id="hero-title">
          도로 위 표지판을<br>
          <span>더 빠르고 명확하게.</span>
        </h2>
        <p class="hero-description">
          이미지를 업로드하면 교통표지판 후보를 찾아 현재 지원하는 독일 GTSRB 43종 범위에서 분석합니다.
          신뢰 기준을 통과하지 못하거나 지원 범위 밖인 이미지는 ‘인식 불가’로 안내합니다.
        </p>

        <div class="hero-actions">
          <a class="button button-secondary" href="#about">서비스 알아보기</a>
        </div>

        <ul class="hero-features" aria-label="주요 기능">
          <li><span aria-hidden="true">01</span> 이미지 업로드</li>
          <li><span aria-hidden="true">02</span> 표지판 분석</li>
          <li><span aria-hidden="true">03</span> 결과 확인·피드백</li>
        </ul>
      </div>

      <a class="section-scroll-link" href="#about" aria-label="아래로 내려가 서비스 작동 방식 보기">
        <span class="scroll-chevrons" aria-hidden="true"><i></i><i></i></span>
      </a>
    </section>

    <section class="story-section about-section" id="about" aria-labelledby="about-title">
      <video class="background-video js-lazy-video" loop muted playsinline preload="none"
          poster="${CP}/resources/picture/bg01.jpg" aria-hidden="true" tabindex="-1">
        <source data-src="${CP}/resources/video/driving.mp4" type="video/mp4">
      </video>
      <div class="media-scrim about-scrim" aria-hidden="true"></div>

      <div class="section-content about-layout">
        <div class="about-copy">
          <p class="eyebrow"><span></span> HOW IT WORKS</p>
          <h2 id="about-title">한 장의 이미지에서<br>교통표지판을 찾아 분류합니다.</h2>
          <p>
            RoadScanner는 이미지 속 표지판 후보를 검출하고 현재 지원하는 독일 GTSRB 43종 안에서 분류합니다.
            업로드부터 결과 확인과 피드백까지 하나의 흐름으로 연결합니다.
          </p>

          <ol class="process-list">
            <li>
              <strong>Upload</strong>
              <span>분석할 교통표지판 이미지를 선택합니다.</span>
            </li>
            <li>
              <strong>Analyze</strong>
              <span>표지판 후보를 찾아 지원하는 43종 범위에서 분류합니다.</span>
            </li>
            <li>
              <strong>Review</strong>
              <span>분류 결과 또는 인식 불가 안내를 확인하고 피드백을 기록합니다.</span>
            </li>
          </ol>
        </div>

        <dl class="metric-grid" aria-label="GTSRB 분류 모델 검증 요약">
          <div class="metric-card metric-card-wide">
            <dt>지원 표지판</dt>
            <dd><strong>43종</strong><span>독일 GTSRB 교통표지판 클래스 · 학습 이미지 39,209장</span></dd>
          </div>
          <div class="metric-card">
            <dt>공식 테스트</dt>
            <dd><strong class="metric-value-compact">12,630</strong><span>GTSRB 표지판 이미지</span></dd>
          </div>
          <div class="metric-card metric-accent">
            <dt>분류 정확도</dt>
            <dd><strong class="metric-value-compact">99.01%</strong><span>12,505 / 12,630 · 잘라낸 표지판 이미지 기준</span></dd>
          </div>
        </dl>
      </div>

      <a class="section-scroll-link" href="#start" aria-label="아래로 내려가 사진 분석 시작 화면 보기">
        <span class="scroll-chevrons" aria-hidden="true"><i></i><i></i></span>
      </a>
    </section>

    <section class="story-section start-section" id="start" aria-labelledby="start-title">
      <div class="section-content start-layout">
        <div class="start-card">
          <p class="eyebrow eyebrow-dark"><span></span> READY TO SCAN?</p>
          <h2 id="start-title">사진을 바로 스캔해 보세요.</h2>
          <p>이미지를 업로드하면 분석 결과 페이지까지 자연스럽게 이어집니다.</p>

          <div class="start-actions">
            <c:choose>
              <c:when test="${empty user}">
                <a class="button button-dark" href="${CP}/login">로그인하고 시작</a>
              </c:when>
              <c:otherwise>
                <a class="button button-dark" href="${CP}/main/preUpload">사진 분석 시작</a>
              </c:otherwise>
            </c:choose>
            <a class="text-link" href="${CP}/qna">Q&amp;A게시판 둘러보기 <span aria-hidden="true">→</span></a>
          </div>
        </div>
      </div>

      <a class="section-scroll-link section-scroll-dark section-scroll-up" href="#intro"
          aria-label="맨 위 인트로 화면으로 이동">
        <span class="scroll-chevrons" aria-hidden="true"><i></i><i></i></span>
      </a>

      <p class="page-footer">© 2023 F1 RoadScanner Project</p>
    </section>
  </main>

</body>
</html>
