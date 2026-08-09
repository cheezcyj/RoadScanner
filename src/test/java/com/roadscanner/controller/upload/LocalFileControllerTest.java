package com.roadscanner.controller.upload;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.roadscanner.service.upload.LocalFileUploadService;

public class LocalFileControllerTest {

    @Test
    public void returnsStoredImageWithNoStoreHeader() {
        LocalFileUploadService service = mock(LocalFileUploadService.class);
        byte[] content = new byte[] { 1, 2, 3 };
        when(service.read("image.png")).thenReturn(content);
        LocalFileController controller = new LocalFileController(service);

        ResponseEntity<byte[]> response = controller.read("image.png");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.IMAGE_PNG, response.getHeaders().getContentType());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertArrayEquals(content, response.getBody());
    }

    @Test
    public void returnsNotFoundForMissingImage() {
        LocalFileUploadService service = mock(LocalFileUploadService.class);
        LocalFileController controller = new LocalFileController(service);

        assertEquals(HttpStatus.NOT_FOUND, controller.read("missing.png").getStatusCode());
    }
}
