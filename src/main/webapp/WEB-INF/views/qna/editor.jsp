<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="editorLabel" value="${inquiryMode ? '문의 내용' : '게시글 내용'}" />
<c:set var="editorPlaceholder" value="${inquiryMode ? '문의 내용을 입력하세요.' : '내용을 입력하세요.'}" />
<div class="qna-editor" data-qna-editor>
    <div class="qna-editor-toolbar" role="toolbar" aria-label="${editorLabel} 서식 도구">
        <div class="qna-editor-tool-group" aria-label="실행 취소와 다시 실행">
            <button type="button" class="qna-editor-tool qna-editor-tool-icon" data-editor-command="undo"
                title="실행 취소" aria-label="실행 취소">↶</button>
            <button type="button" class="qna-editor-tool qna-editor-tool-icon" data-editor-command="redo"
                title="다시 실행" aria-label="다시 실행">↷</button>
        </div>
        <div class="qna-editor-tool-group" aria-label="문단 서식">
            <button type="button" class="qna-editor-tool qna-editor-tool-text" data-editor-command="formatBlock"
                data-editor-value="p" title="본문">본문</button>
            <button type="button" class="qna-editor-tool qna-editor-tool-text" data-editor-command="formatBlock"
                data-editor-value="h2" title="제목">제목</button>
        </div>
        <div class="qna-editor-tool-group" aria-label="글자 서식">
            <button type="button" class="qna-editor-tool qna-editor-tool-bold" data-editor-command="bold"
                title="굵게" aria-label="굵게" aria-pressed="false">B</button>
            <button type="button" class="qna-editor-tool qna-editor-tool-italic" data-editor-command="italic"
                title="기울임" aria-label="기울임" aria-pressed="false">I</button>
            <button type="button" class="qna-editor-tool qna-editor-tool-underline" data-editor-command="underline"
                title="밑줄" aria-label="밑줄" aria-pressed="false">U</button>
        </div>
        <div class="qna-editor-tool-group" aria-label="목록과 인용">
            <button type="button" class="qna-editor-tool qna-editor-tool-icon" data-editor-command="insertUnorderedList"
                title="글머리 기호 목록" aria-label="글머리 기호 목록" aria-pressed="false">•≡</button>
            <button type="button" class="qna-editor-tool qna-editor-tool-icon" data-editor-command="insertOrderedList"
                title="번호 목록" aria-label="번호 목록" aria-pressed="false">1.</button>
            <button type="button" class="qna-editor-tool qna-editor-tool-icon" data-editor-command="formatBlock"
                data-editor-value="blockquote" title="인용" aria-label="인용">❝</button>
        </div>
        <div class="qna-editor-tool-group" aria-label="서식 지우기">
            <button type="button" class="qna-editor-tool qna-editor-tool-clear" data-editor-command="removeFormat"
                title="서식 지우기" aria-label="서식 지우기">Tx</button>
        </div>
    </div>
    <div class="qna-editor-surface" contenteditable="true" role="textbox" aria-multiline="true"
        aria-label="${editorLabel}" data-placeholder="${editorPlaceholder}"></div>
    <textarea id="content" class="qna-editor-source" data-maxlength="10000" hidden aria-hidden="true" tabindex="-1"><c:out value="${editorInitialContent}"/></textarea>
    <div class="qna-editor-status">
        <span class="qna-editor-help">
            <c:choose>
                <c:when test="${inquiryMode}">연락처나 비밀번호 등 민감한 정보는 입력하지 마세요.</c:when>
                <c:otherwise>선택한 글자나 문단에 서식을 적용할 수 있습니다.</c:otherwise>
            </c:choose>
        </span>
        <output class="qna-editor-character-count" data-editor-count aria-live="polite">0 / 10,000자</output>
    </div>
</div>
