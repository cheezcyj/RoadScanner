package com.roadscanner.dto.qna;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
public class QuestionUpdateRequestDTO {

    // SaveRequestDto와 비슷 하지만 작성자는 수정될 수 없음.
    private Integer category;
    private String title;
    private MultipartFile attachFile;
    private String content;

}
