package com.roadscanner.dto.qna;

import com.roadscanner.domain.qna.AnswerVO;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
public class AnswerResponseDTO {

    /**
     * 답변 상세화면
     */

    private String id;
    private String content;
    private String createDate;
    private String updateDate;

    public AnswerResponseDTO(AnswerVO vo) {
        this.id = vo.getId();
        this.content = vo.getContent();

        // 날짜 formatting
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy/MM/dd HH:mm:ss");

        this.createDate = vo.getCreateDate().format(formatter);

        if (vo.getUpdateDate() != null && !vo.getUpdateDate().equals(vo.getCreateDate())) {
            this.updateDate = vo.getUpdateDate().format(formatter);
        } else {
            this.updateDate = null; // 수정일이 없는 경우
        }
    }

} // class end
