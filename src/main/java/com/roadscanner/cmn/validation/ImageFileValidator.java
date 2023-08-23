package com.roadscanner.cmn.validation;

import org.springframework.web.multipart.MultipartFile;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ImageFileValidator implements ConstraintValidator<ImageFile, MultipartFile> {

    private long maxSize;

    @Override
    public void initialize(ImageFile constraintAnnotation) {
        maxSize = constraintAnnotation.maxSize();
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null) {
            return true; // 필수가 아닌 경우
        }

        // 파일 크기 검증
        if (file.getSize() > maxSize) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("파일 크기는 " + maxSize / 1024 / 1024 + "MB를 초과할 수 없습니다.")
                    .addConstraintViolation();
            return false;
        }

        // 이미지 파일 타입 검증
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image");

    }
}
