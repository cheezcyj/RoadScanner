package com.roadscanner.config;

import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.HttpServerErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BoundedAnalysisHttpClientTest {

    @Test
    public void boundedConverterReadsSmallResponse() throws Exception {
        BoundedStringHttpMessageConverter converter = new BoundedStringHttpMessageConverter();
        MockHttpInputMessage input = new MockHttpInputMessage(
                "42".getBytes(StandardCharsets.UTF_8));
        input.getHeaders().setContentType(MediaType.TEXT_PLAIN);

        assertThat(converter.read(String.class, input)).isEqualTo("42");
    }

    @Test
    public void boundedConverterRejectsLargeResponse() {
        BoundedStringHttpMessageConverter converter = new BoundedStringHttpMessageConverter();
        MockHttpInputMessage input = new MockHttpInputMessage(
                new byte[BoundedStringHttpMessageConverter.MAX_RESPONSE_BYTES + 1]);

        assertThatThrownBy(() -> converter.read(String.class, input))
                .isInstanceOf(HttpMessageNotReadableException.class)
                .hasMessageContaining("allowed size");
    }

    @Test
    public void errorHandlerClassifiesStatusWithoutIncludingBody() {
        StatusOnlyResponseErrorHandler handler = new StatusOnlyResponseErrorHandler();
        MockClientHttpResponse response = new MockClientHttpResponse(
                "private upstream details".getBytes(StandardCharsets.UTF_8),
                HttpStatus.SERVICE_UNAVAILABLE);

        assertThatThrownBy(() -> handler.handleError(response))
                .isInstanceOf(HttpServerErrorException.class)
                .hasMessageNotContaining("private upstream details");
    }
}
