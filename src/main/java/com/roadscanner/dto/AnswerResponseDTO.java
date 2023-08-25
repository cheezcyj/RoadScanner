package com.roadscanner.dto;

import com.roadscanner.domain.qna.AnswerVO;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AnswerResponseDTO {

    /**
     * 답변 상세화면
     */

    private String id;
    private String content;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;

    public AnswerResponseDTO(AnswerVO vo) {
        this.id = vo.getId();
        this.content = vo.getContent();
        this.createDate = vo.getCreateDate();
        this.updateDate = vo.getUpdateDate();
    }

}
