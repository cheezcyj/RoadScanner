package com.roadscanner.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import javax.validation.ConstraintValidatorContext;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.roadscanner.cmn.validation.ImageFile;
import com.roadscanner.cmn.validation.ImageFileValidator;

public class ImageFileValidatorTest {

    private ImageFileValidator validator;

    @Before
    public void setUp() throws Exception {
        Field field = UploadFixture.class.getDeclaredField("file");
        ImageFile annotation = field.getAnnotation(ImageFile.class);
        validator = new ImageFileValidator();
        validator.initialize(annotation);
    }

    @Test
    public void nullFileIsValidBecauseConstraintIsOptional() {
        assertTrue(validator.isValid(null, null));
    }

    @Test
    public void safeImageIsValid() {
        assertTrue(validator.isValid(image("road.png", "image/png; charset=binary", new byte[] { 1 }), null));
    }

    @Test
    public void emptyImageIsInvalid() {
        assertFalse(validator.isValid(image("road.png", "image/png", new byte[0]), null));
    }

    @Test
    public void oversizedImageIsInvalidAndReportsSizeMessage() {
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder builder =
                mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
        when(builder.addConstraintViolation()).thenReturn(context);

        assertFalse(validator.isValid(image("road.png", "image/png", new byte[] { 1, 2, 3, 4, 5 }), context));
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    public void nonImageContentTypeIsInvalid() {
        assertFalse(validator.isValid(image("road.png", "application/octet-stream", new byte[] { 1 }), null));
    }

    @Test
    public void imageLikeContentTypePrefixIsInvalid() {
        assertFalse(validator.isValid(image("road.png", "image-malicious", new byte[] { 1 }), null));
    }

    @Test
    public void pathInOriginalFilenameIsInvalid() {
        assertFalse(validator.isValid(image("../road.png", "image/png", new byte[] { 1 }), null));
    }

    @Test
    public void htmlAttributeBreakingFilenameIsInvalid() {
        assertFalse(validator.isValid(
                image("road\" autofocus onfocus=\"alert(1).png", "image/png", new byte[] { 1 }),
                null));
    }

    @Test
    public void unicodeLettersAndSafeSeparatorsAreValid() {
        assertTrue(validator.isValid(image("도로_표지판-01.png", "image/png", new byte[] { 1 }), null));
    }

    @Test
    public void internalSpacesInDisplayFilenameAreValid() {
        assertTrue(validator.isValid(image("road sign.png", "image/png", new byte[] { 1 }), null));
    }

    @Test
    public void unsupportedExtensionIsInvalid() {
        assertFalse(validator.isValid(image("road.exe", "image/png", new byte[] { 1 }), null));
    }

    @Test
    public void activeSvgContentIsInvalid() {
        assertFalse(validator.isValid(image("road.svg", "image/svg+xml", new byte[] { 1 }), null));
    }

    private MockMultipartFile image(String originalFilename, String contentType, byte[] content) {
        return new MockMultipartFile("fileUpload", originalFilename, contentType, content);
    }

    private static class UploadFixture {
        @ImageFile(maxSize = 4)
        private MultipartFile file;
    }
}
