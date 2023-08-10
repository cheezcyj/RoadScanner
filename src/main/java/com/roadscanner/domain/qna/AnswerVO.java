package com.roadscanner.domain.qna;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
public class AnswerVO {

    private Long no;  // 게시글 번호
    private String id;  // id
    private String content;  // 내용
    private LocalDateTime createDate;  // 작성일
    private LocalDateTime updateDate;  // 수정일

    public AnswerVO(Long no, String id, String content) {
        this.no = no;
        this.id = id;
        this.content = content;
    }

    public void update(String content){
        this.content = content;
    }

}
