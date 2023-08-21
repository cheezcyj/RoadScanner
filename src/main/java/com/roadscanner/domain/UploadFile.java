package com.roadscanner.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * uploadFileName = 고객이 업로드한 파일명, storeFileName = 서버 내부에서 관리하는 파일
 * 고객이 업로드한 파일명으로 서버내부에 저장하면 절대안됨.
 */
@Data
@AllArgsConstructor
public class UploadFile {

    private String uploadFileName;
    private String storeFileName;
    private String url;

}
