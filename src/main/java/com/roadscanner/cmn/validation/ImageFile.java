package com.roadscanner.cmn.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ImageFileValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ImageFile {
    String message() default "이미지 파일만 업로드 가능합니다.";
    long maxSize() default 5 * 1024 * 1024; // 기본 크기 5MB
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
