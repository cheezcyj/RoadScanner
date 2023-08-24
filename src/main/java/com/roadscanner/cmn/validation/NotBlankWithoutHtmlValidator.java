package com.roadscanner.cmn.validation;

import org.jsoup.Jsoup;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class NotBlankWithoutHtmlValidator implements ConstraintValidator<NotBlankWithoutHtml, String> {

    @Override
    public void initialize(NotBlankWithoutHtml constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }

        // HTML 태그를 제거하고 텍스트만 추출
        String textWithoutHtml = Jsoup.parse(value).text();

        return !textWithoutHtml.trim().isEmpty();
    }
}
