package com.roadscanner.dto;

import com.roadscanner.cmn.validation.NotBlankWithoutHtml;
import com.roadscanner.cmn.validation.QuestionContentLimits;
import com.roadscanner.cmn.validation.VisibleTextSize;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter @Setter
@NoArgsConstructor
public class QuestionUpdateRequestDTO {

    // SaveRequestDto와 비슷 하지만 작성자는 수정될 수 없음.
    private Integer category;
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 45, message = "제목은 45자 이하여야 합니다.")
    private String title;
    private Long idx;
    @NotBlankWithoutHtml
    @Size(max = QuestionContentLimits.MAX_RAW_LENGTH, message = "내용 데이터가 허용 크기를 초과했습니다.")
    @VisibleTextSize(max = QuestionContentLimits.MAX_VISIBLE_LENGTH, message = "내용은 10000자 이하여야 합니다.")
    private String content;

    public QuestionUpdateRequestDTO(Integer category, String title, Long idx, String content) {
        this.category = category;
        this.title = title;
        this.idx = idx;
        this.content = content;
    }

}
