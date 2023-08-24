package com.roadscanner.domain.qna;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Domain 클래스, 데이터베이스와 매칭
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionVO {

    private Long no; // 번호
    private Integer category; // 카테고리
    private Integer views; // 조회수
    private String id; // 사용자 아이디 -> userId
    private String title; // 제목
    private String content; // 내용
    private String originalFilename; // 원본 파일명
    private String storeFilename; // 서버에 저장되는 파일명
    private String imageUrl; // S3에 저장된 URL
    private LocalDateTime createDate; // 작성일
    private LocalDateTime updateDate; // 수정일

    // no, idx, createDate, updateDate는 데이터베이스에서 값을 자동으로 주입 => 생성자로 값 주입 안함!
    // 필수로 값이 주입되야 하는 필드만 생성
    public QuestionVO(Integer category, String id, String title, String content) {
        this.category = category;
        this.id = id;
        this.title = title;
        this.content = content;
    }

    public void update(Integer category, String title, Long idx, String content) {
        this.category = category;
        this.title = title;
        this.content = content;
    }

    // 게시글 분류만 변경하는 메서드
    public void updateCategory(Integer category) {
        this.category = category;
    }

}
