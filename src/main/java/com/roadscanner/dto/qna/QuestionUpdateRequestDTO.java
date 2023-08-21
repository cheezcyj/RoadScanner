package com.roadscanner.dto.qna;

import lombok.*;

@Data
@NoArgsConstructor
public class QuestionUpdateRequestDTO {

    // SaveRequestDto와 비슷 하지만 작성자는 수정될 수 없음.
    private Integer category;
    private String title;
    private String content;

}
