package com.roadscanner.dto.qna;

import com.roadscanner.domain.qna.QuestionVO;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class QuestionSaveRequestDTO {

    private Integer category; // enum 적용
    private String id; // 유저 아이디로 변경될것
    private MultipartFile attachFile;
    private String title;
    private String content;

    /*
    public QuestionVO toEntity() {
        return new QuestionVO(
                this.getCategory(),
                this.getId(),
                this.getTitle(),
                this.getContent());
    }
    */
}
