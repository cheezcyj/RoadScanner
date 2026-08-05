package com.roadscanner.dto;

import com.roadscanner.domain.qna.QuestionVO;
import com.roadscanner.cmn.validation.NotBlankWithoutHtml;
import com.roadscanner.cmn.validation.QuestionContentLimits;
import com.roadscanner.cmn.validation.VisibleTextSize;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
public class QuestionSaveRequestDTO {

    private Integer category; // enum 적용
    private String id; // 유저 아이디로 변경될것
    private Long idx; // 첨부파일
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 45, message = "제목은 45자 이하여야 합니다.")
    private String title;
    @NotBlankWithoutHtml
    @Size(max = QuestionContentLimits.MAX_RAW_LENGTH, message = "내용 데이터가 허용 크기를 초과했습니다.")
    @VisibleTextSize(max = QuestionContentLimits.MAX_VISIBLE_LENGTH, message = "내용은 10000자 이하여야 합니다.")
    private String content;

    public QuestionVO toEntity() {
        return new QuestionVO(this.getCategory(), this.getId(), this.getIdx(), this.getTitle(), this.getContent());
    }
}
