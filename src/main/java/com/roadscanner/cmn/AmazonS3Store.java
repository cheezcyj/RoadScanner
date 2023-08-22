package com.roadscanner.cmn;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.roadscanner.domain.UploadFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class AmazonS3Store {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * 업로드 파일로 변환함
     * @param multipartFile
     * @return
     */
    public UploadFile storeFile(MultipartFile multipartFile) throws IOException {
        if (multipartFile.isEmpty()) {
            return null;
        }

        // 사용자가 업로드한 파일명 ex) roadscanner.png
        String originalFilename = multipartFile.getOriginalFilename();
        // 서버에 저장하는 파일명 uuid + ext(확장자)
        String storeFileName = createStoreFileName(originalFilename);

        // GPT님께서 요청하심.
        File file = convertMultipartFileToFile(multipartFile);
        amazonS3.putObject(new PutObjectRequest(bucket, storeFileName, file));
        file.delete(); // 임시 파일 삭제

        String fileUrl = amazonS3.getUrl(bucket, storeFileName).toString();

        return new UploadFile(originalFilename, storeFileName, fileUrl);
    }

    /**
     * S3에 업로드를 하기위해서는 File 객체가 필요하다. 메모리에 존재하는 MultipartFile을 File 객체로 변환해야 한다.
     * 메모리 용량을 줄이기 위해 임시파일을 만들고 삭제하는 전략을 사용한다고 한다.
     * 서버의 로컬, 임시 디렉토리에 임시 파일을 만들고, S3에 업로드 되 삭제 시킨다.
     * @param multipartFile
     * @return
     * @throws IOException
     */
    private File convertMultipartFileToFile(MultipartFile multipartFile) throws IOException {
        File tempFile = File.createTempFile("temp", null);
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(multipartFile.getBytes());
        }
        return tempFile;
    }

    /**
     * 서버 내부에서 관리하는 파일명을 유일한 이름을 생성하는 UUID를 사용해 충돌하지 않게한다.
     * @param originalFilename
     * @return
     */
    private String createStoreFileName(String originalFilename) {
        String uuid = UUID.randomUUID().toString();
        String ext = extractExt(originalFilename);
        return uuid + "." + ext;
    }

    /**
     * 확장자를 별도로 추출해서 서버 내부에서 관리하는 파일명에 붙여준다. (관리하기 용이함)
     * @param originalFilename
     * @return
     */
    private String extractExt(String originalFilename) {
        int pos = originalFilename.lastIndexOf(".");
        return originalFilename.substring(pos + 1);
    }

    /**
     * 서버에 저장된 파일명으로 파일을 찾고 해당 파일을 삭제
     * @param storeFileName
     */
    public void deleteFile(String storeFileName) {
        amazonS3.deleteObject(bucket, storeFileName);
    }

}