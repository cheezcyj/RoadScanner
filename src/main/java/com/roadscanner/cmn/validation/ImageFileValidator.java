package com.roadscanner.cmn.validation;

import org.springframework.web.multipart.MultipartFile;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class ImageFileValidator implements ConstraintValidator<ImageFile, MultipartFile> {

    private static final Set<String> SUPPORTED_CONTENT_TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/bmp"
    )));

    private static final Set<String> SUPPORTED_EXTENSIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "bmp"
    )));

    private static final Pattern SAFE_FILENAME = Pattern.compile(
            "[\\p{L}\\p{N}](?:[\\p{L}\\p{N} ._-]{0,253}[\\p{L}\\p{N}])?");

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

        if (file.isEmpty()) {
            return false;
        }

        // 파일 크기 검증
        if (file.getSize() > maxSize) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("파일 크기는 " + maxSize / 1024 / 1024 + "MB를 초과할 수 없습니다.")
                    .addConstraintViolation();
            return false;
        }

        return hasSafeImageFilename(file.getOriginalFilename())
                && isSupportedContentType(file.getContentType());

    }

    public static boolean hasSafeImageFilename(String filename) {
        if (filename == null
                || filename.isEmpty()
                || filename.length() > 255
                || !SAFE_FILENAME.matcher(filename).matches()) {
            return false;
        }

        int extensionSeparator = filename.lastIndexOf('.');
        if (extensionSeparator <= 0 || extensionSeparator == filename.length() - 1) {
            return false;
        }

        String extension = filename.substring(extensionSeparator + 1).toLowerCase(Locale.ROOT);
        return SUPPORTED_EXTENSIONS.contains(extension);
    }

    public static boolean isSupportedContentType(String contentType) {
        if (contentType == null) {
            return false;
        }

        int parameterSeparator = contentType.indexOf(';');
        String normalizedContentType = (parameterSeparator >= 0
                ? contentType.substring(0, parameterSeparator)
                : contentType).trim().toLowerCase(Locale.ROOT);

        return SUPPORTED_CONTENT_TYPES.contains(normalizedContentType);
    }
}
