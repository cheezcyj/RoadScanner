package com.roadscanner.dto.qna;

import com.roadscanner.domain.qna.QuestionVO;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
public class QuestionResponseDTO {

    /**
     * 상세화면을 정의하는 DTO
     */

    private Long no;
    private Integer category;
    private String title;
    private String id; // 작성자 -> 유저 아이디
    private Long idx;
    private String createDate;
    private String updateDate;
    private int views;
    private String content;

    public QuestionResponseDTO(QuestionVO vo) {
        this.no = vo.getNo();
        this.category = vo.getCategory();
        this.title = vo.getTitle();
        this.id = vo.getId();
        this.idx = vo.getIdx();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss");
        this.createDate = vo.getCreateDate().format(formatter);

        if (vo.getUpdateDate() != null && !vo.getUpdateDate().equals(vo.getCreateDate())) {
            this.updateDate = vo.getUpdateDate().format(formatter);
        } else {
            this.updateDate = null; // 수정일이 없는 경우
        }

        this.views = vo.getViews();
        this.content = vo.getContent();
    }

}
