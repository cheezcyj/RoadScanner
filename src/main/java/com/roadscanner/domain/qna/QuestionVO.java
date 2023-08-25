package com.roadscanner.domain.qna;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// domain 클래스 => 데이터베이스랑 1대1 매칭
@Getter @Setter
@NoArgsConstructor
public class QuestionVO {

    private Long no; // 번호
    private Integer category; // 카테고리
    private Integer views; // 조회수
    private Long idx; // 이미지ID
    private String id; // 사용자 아이디 -> userId
    private String title; // 제목
    private String content; // 내용
    private LocalDateTime createDate; // 작성일
    private LocalDateTime updateDate; // 수정일

    // no, idx, createDate, updateDate는 데이터베이스에서 값을 자동으로 주입 => 생성자로 값 주입 안함!
    // 필수로 값이 주입되야 하는 필드만 생성
    public QuestionVO(Integer category, String id, Long idx, String title, String content) {
        this.category = category;
        this.id = id;
        this.idx = idx;
        this.title = title;
        this.content = content;
    }

    public void update(Integer category, String title, Long idx, String content) {
        this.category = category;
        this.title = title;
        this.idx = idx;
        this.content = content;
    }

    // 게시글 분류만 변경하는 메서드
    public void updateCategory(Integer category) {
        this.category = category;
    }

}
