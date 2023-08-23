package com.roadscanner.validation;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.validation.DefaultMessageCodesResolver;
import org.springframework.validation.MessageCodesResolver;

import static org.assertj.core.api.Assertions.*;

/**
 * MessageCodesResolver
 * 검증 오류 코드로 메시지 코드들을 생성한다.
 * 주로 ObjectError, FieldError와 함께 사용
 */
public class MessageCodesResolverTest {

    MessageCodesResolver codesResolver = new DefaultMessageCodesResolver();

    @Test
    public void messageCodesResolverObject() {
        String[] messageCodes = codesResolver.resolveMessageCodes("required", "question");
        assertThat(messageCodes).containsExactly("required.question", "required");
    }

    @Test
    public void messageCodesResolverField() {
        String[] messageCodes = codesResolver.resolveMessageCodes("required", "question", "title", String.class);
        assertThat(messageCodes).containsExactly(
                "required.question.title",
                "required.title",
                "required.java.lang.String",
                "required"
        );
    }
}
