package com.roadscanner.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.roadscanner.cmn.validation.NotBlankWithoutHtmlValidator;

public class NotBlankWithoutHtmlValidatorTest {

    private NotBlankWithoutHtmlValidator validator;

    @Before
    public void setUp() {
        validator = new NotBlankWithoutHtmlValidator();
    }

    @Test
    public void nullIsInvalid() {
        assertFalse(validator.isValid(null, null));
    }

    @Test
    public void plainTextIsValid() {
        assertTrue(validator.isValid("도로 파손 신고", null));
    }

    @Test
    public void textWrappedInHtmlIsValid() {
        assertTrue(validator.isValid("<p><strong>포트홀</strong>이 있습니다.</p>", null));
    }

    @Test
    public void formattingTagsWithoutTextAreInvalid() {
        assertFalse(validator.isValid("<p><br></p>", null));
    }

    @Test
    public void nonBreakingSpacesAreInvalid() {
        assertFalse(validator.isValid("<p>&nbsp;&#160;</p>", null));
    }

    @Test
    public void invisibleFormattingCharactersAreInvalid() {
        assertFalse(validator.isValid("\u200B\u200C\u200D\uFEFF", null));
    }

    @Test
    public void mediaOnlyHtmlIsInvalid() {
        assertFalse(validator.isValid("<img src=\"road.png\">", null));
    }
}
