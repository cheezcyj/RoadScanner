package com.roadscanner.dto.qna;

import com.roadscanner.domain.qna.QuestionVO;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.format.DateTimeFormatter;

/**
 * 상세화면을 정의
 */
@Data
public class QuestionResponseDTO {

    private Long no;
    private Integer category;
    private String title;
    private String id; // 작성자 -> 유저 아이디
    private String createDate;
    private String updateDate;
    private int views;
    private String content;
    private String originalFilename; // 원본 파일명
    private String storeFilename; // 서버에 저장되는 파일명
    private String imageUrl;

    public QuestionResponseDTO(QuestionVO vo) {
        this.no = vo.getNo();
        this.category = vo.getCategory();
        this.title = vo.getTitle();
        this.id = vo.getId();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss");
        this.createDate = vo.getCreateDate().format(formatter);

        if (vo.getUpdateDate() != null && !vo.getUpdateDate().equals(vo.getCreateDate())) {
            this.updateDate = vo.getUpdateDate().format(formatter);
        } else {
            this.updateDate = null; // 수정일이 없는 경우
        }

        this.views = vo.getViews();
        this.content = vo.getContent();

        this.originalFilename = vo.getOriginalFilename();
        this.storeFilename = vo.getStoreFilename();

        if (vo.getImageUrl() != null) {
            this.imageUrl = vo.getImageUrl();
        } else {
            this.imageUrl = null;
        }

    }

}
