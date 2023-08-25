package com.roadscanner.dto.qna;

import lombok.*;

@ToString
@Getter @Setter
@NoArgsConstructor
public class QuestionUpdateRequestDTO {

    // SaveRequestDto와 비슷 하지만 작성자는 수정될 수 없음.
    private Integer category;
    private String title;
    private Long idx;
    private String content;

    public QuestionUpdateRequestDTO(Integer category, String title, Long idx, String content) {
        this.category = category;
        this.title = title;
        this.idx = idx;
        this.content = content;
    }

}
