package com.roadscanner.dto.qna;

import com.roadscanner.cmn.validation.ImageFile;
import com.roadscanner.cmn.validation.NotBlankWithoutHtml;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
public class QuestionUpdateRequestDTO {

    // SaveRequestDto와 비슷 하지만 작성자는 수정될 수 없음.
    private Integer category;

    @Size(min = 1, max = 15, message = "제목은 필수입니다.")
    private String title;

    @ImageFile
    private MultipartFile attachFile;

//    @NotBlank(message = "내용은 필수입니다.")
    @NotBlankWithoutHtml(message = "내용은 필수입니다.")
    private String content;

    // 기본값 : false, 업데이트 되었는지 확인
    private boolean isFileChanged;

}
