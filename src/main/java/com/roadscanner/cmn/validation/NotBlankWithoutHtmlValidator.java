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
        if (value.length() > QuestionContentLimits.MAX_RAW_LENGTH) {
            // Size validation reports the user-facing error.  Reject here too,
            // before Jsoup parses attacker-controlled oversized markup.
            return false;
        }

        // HTML 태그를 제거하고 텍스트만 추출
        String textWithoutHtml = Jsoup.parse(value).text();

        for (int index = 0; index < textWithoutHtml.length();) {
            int codePoint = textWithoutHtml.codePointAt(index);
            index += Character.charCount(codePoint);
            boolean ignoredFormattingCharacter = codePoint == 0x200B
                    || codePoint == 0x200C
                    || codePoint == 0x200D
                    || codePoint == 0xFEFF;
            if (!Character.isWhitespace(codePoint)
                    && !Character.isSpaceChar(codePoint)
                    && !ignoredFormattingCharacter) {
                return true;
            }
        }

        return false;
    }
}
