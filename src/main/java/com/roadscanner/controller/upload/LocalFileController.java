package com.roadscanner.controller.upload;

import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.roadscanner.service.upload.LocalFileUploadService;

@RestController
@Profile("local")
public class LocalFileController {

    private final LocalFileUploadService fileUploadService;

    public LocalFileController(LocalFileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @GetMapping("/local-files/{name:.+}")
    public ResponseEntity<byte[]> read(@PathVariable String name) {
        byte[] content = fileUploadService.read(name);
        if (content == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType(name));
        headers.setCacheControl(CacheControl.noStore());
        headers.setContentLength(content.length);
        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }

    private MediaType mediaType(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".bmp")) {
            return MediaType.parseMediaType("image/bmp");
        }
        return MediaType.IMAGE_JPEG;
    }
}
