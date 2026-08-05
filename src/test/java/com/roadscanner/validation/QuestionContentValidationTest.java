package com.roadscanner.validation;

import com.roadscanner.dto.QuestionSaveRequestDTO;
import com.roadscanner.dto.QuestionUpdateRequestDTO;
import org.junit.AfterClass;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class QuestionContentValidationTest {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    @AfterClass
    public static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    public void saveAcceptsTenThousandVisibleCharactersDespiteHtmlMarkup() {
        QuestionSaveRequestDTO request = validSaveRequest("<p><strong>" + repeat('가', 10000) + "</strong></p>");

        assertThat(contentViolations(request)).isEmpty();
    }

    @Test
    public void formattingAndBlockElementsDoNotConsumeTheVisibleTextLimit() {
        String content = "<p>" + repeat('가', 5000) + "</p><blockquote>"
                + repeat('나', 5000) + "</blockquote>";

        assertThat(contentViolations(validSaveRequest(content))).isEmpty();
    }

    @Test
    public void saveRejectsMoreThanTenThousandVisibleCharacters() {
        QuestionSaveRequestDTO request = validSaveRequest("<p>" + repeat('가', 10001) + "</p>");

        assertThat(contentViolations(request)).extracting(ConstraintViolation::getMessage)
                .contains("내용은 10000자 이하여야 합니다.");
    }

    @Test
    public void updateUsesTheSameVisibleTextLimitAsSave() {
        QuestionUpdateRequestDTO request = new QuestionUpdateRequestDTO(
                40, "제목", null, "<blockquote>" + repeat('가', 10001) + "</blockquote>");

        assertThat(contentViolations(request)).extracting(ConstraintViolation::getMessage)
                .contains("내용은 10000자 이하여야 합니다.");
    }

    @Test
    public void rejectsExcessiveFormattingMarkupEvenWhenVisibleTextIsShort() {
        String content = "<p>x</p>" + repeat("<strong></strong>", 16000);

        assertThat(contentViolations(validSaveRequest(content)))
                .extracting(ConstraintViolation::getMessage)
                .contains("내용 데이터가 허용 크기를 초과했습니다.");
    }

    private QuestionSaveRequestDTO validSaveRequest(String content) {
        QuestionSaveRequestDTO request = new QuestionSaveRequestDTO();
        request.setCategory(40);
        request.setTitle("제목");
        request.setContent(content);
        return request;
    }

    private Set<ConstraintViolation<Object>> contentViolations(Object request) {
        return VALIDATOR.validate(request, javax.validation.groups.Default.class).stream()
                .filter(violation -> "content".equals(violation.getPropertyPath().toString()))
                .collect(java.util.stream.Collectors.toSet());
    }

    private String repeat(char value, int count) {
        char[] values = new char[count];
        java.util.Arrays.fill(values, value);
        return new String(values);
    }

    private String repeat(String value, int count) {
        StringBuilder repeated = new StringBuilder(value.length() * count);
        for (int index = 0; index < count; index++) {
            repeated.append(value);
        }
        return repeated.toString();
    }
}
