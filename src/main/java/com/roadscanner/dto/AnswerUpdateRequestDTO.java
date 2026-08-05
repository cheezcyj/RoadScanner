package com.roadscanner.dto;

import com.roadscanner.cmn.validation.NotBlankWithoutHtml;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Size;

@Getter @Setter
@NoArgsConstructor
public class AnswerUpdateRequestDTO {

    @NotBlankWithoutHtml
    @Size(max = 10000, message = "내용은 10000자 이하여야 합니다.")
    private String content;

    public AnswerUpdateRequestDTO(String content) {
        this.content = content;
    }

}
