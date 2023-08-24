package com.roadscanner.dto.qna;

import com.roadscanner.cmn.validation.ImageFile;
import com.roadscanner.cmn.validation.NotBlankWithoutHtml;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class QuestionSaveRequestDTO {

    private Integer category; // enum 적용
    private String id; // 유저 아이디로 변경될것

    @ImageFile
    private MultipartFile attachFile;

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 15, message = "제목은 15글자 이하여야 합니다.")
    private String title;

//    @NotBlank(message = "내용은 필수입니다.")
    @NotBlankWithoutHtml(message = "내용은 필수입니다.")
    private String content;

}
