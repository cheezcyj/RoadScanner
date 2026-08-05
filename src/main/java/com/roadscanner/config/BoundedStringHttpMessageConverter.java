package com.roadscanner.config;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;

/**
 * String converter for the dedicated analysis client with a strict response cap.
 */
public final class BoundedStringHttpMessageConverter
        extends AbstractHttpMessageConverter<String> {

    static final int MAX_RESPONSE_BYTES = 64;

    public BoundedStringHttpMessageConverter() {
        super(StandardCharsets.UTF_8, MediaType.TEXT_PLAIN, MediaType.ALL);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return String.class == clazz;
    }

    @Override
    protected String readInternal(Class<? extends String> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        byte[] buffer = new byte[32];
        ByteArrayOutputStream output = new ByteArrayOutputStream(MAX_RESPONSE_BYTES);
        int total = 0;
        int read;
        while ((read = inputMessage.getBody().read(buffer)) != -1) {
            total += read;
            if (total > MAX_RESPONSE_BYTES) {
                throw new HttpMessageNotReadableException(
                        "Analysis response exceeded the allowed size", inputMessage);
            }
            output.write(buffer, 0, read);
        }
        return new String(output.toByteArray(), responseCharset(inputMessage));
    }

    @Override
    protected void writeInternal(String value, HttpOutputMessage outputMessage) throws IOException {
        outputMessage.getBody().write(value.getBytes(responseCharset(outputMessage)));
    }

    private Charset responseCharset(org.springframework.http.HttpMessage message) {
        MediaType contentType = message.getHeaders().getContentType();
        return contentType != null && contentType.getCharset() != null
                ? contentType.getCharset()
                : StandardCharsets.UTF_8;
    }
}
