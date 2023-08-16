package com.roadscanner.dto.qna;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter @Setter
@NoArgsConstructor
public class AnswerUpdateRequestDTO {

    private String content;

    public AnswerUpdateRequestDTO(String content) {
        this.content = content;
    }

}
