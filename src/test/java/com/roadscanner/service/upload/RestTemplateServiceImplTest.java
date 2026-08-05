package com.roadscanner.service.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.roadscanner.service.upload.AnalysisApiException.FailureType;

@RunWith(MockitoJUnitRunner.class)
public class RestTemplateServiceImplTest {

    private static final String API_URL = "https://analysis.example.test/infer";
    private static final String IMAGE_URL = "https://cdn.example.test/road.png";
    private static final String CATALOG_SHA256 =
            "A5001A6CFAC4CF1C1135ABA53312226A59617FA635DF09B1C9B7B9F037A01F8E";

    @Mock
    private RestTemplate restTemplate;

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void sendsJsonRequestAndReturnsResponseBody() {
        when(exchange()).thenReturn(ok("7"));
        RestTemplateServiceImpl service = service(2);

        String response = service.callFlaskApi(IMAGE_URL);

        assertThat(response).isEqualTo("7");
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(eq(API_URL), eq(HttpMethod.POST), entityCaptor.capture(), eq(String.class));
        HttpEntity<Map<String, String>> entity = entityCaptor.getValue();
        assertThat(entity.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(entity.getHeaders().getAccept()).containsExactly(MediaType.TEXT_PLAIN);
        assertThat(entity.getBody()).containsEntry("image_url", IMAGE_URL);
    }

    @Test
    public void retriesServerErrorAndReturnsSuccessfulRetry() {
        when(exchange())
                .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE))
                .thenReturn(ok("11"));
        RestTemplateServiceImpl service = service(2);

        assertThat(service.callFlaskApi(IMAGE_URL)).isEqualTo("11");

        verify(restTemplate, times(2)).exchange(
                eq(API_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }

    @Test
    public void exhaustedReadTimeoutIsClassifiedWithoutEndpointDetails() {
        ResourceAccessException timeout = new ResourceAccessException(
                "request failed", new SocketTimeoutException("read timed out"));
        when(exchange()).thenThrow(timeout);
        RestTemplateServiceImpl service = service(2);

        AnalysisApiException failure = captureFailure(service);

        assertThat(failure.getFailureType()).isEqualTo(FailureType.TIMEOUT);
        assertThat(failure.isRetryable()).isTrue();
        assertThat(failure.getMessage()).doesNotContain(API_URL).doesNotContain(IMAGE_URL);
        verify(restTemplate, times(2)).exchange(
                eq(API_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }

    @Test
    public void exhaustedConnectionFailureIsClassifiedAndBounded() {
        ResourceAccessException connection = new ResourceAccessException(
                "request failed", new ConnectException("connection refused"));
        when(exchange()).thenThrow(connection);
        RestTemplateServiceImpl service = service(3);

        AnalysisApiException failure = captureFailure(service);

        assertThat(failure.getFailureType()).isEqualTo(FailureType.CONNECTION);
        verify(restTemplate, times(3)).exchange(
                eq(API_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }

    @Test
    public void exhaustedServerErrorsRetainOnlyStatusClassification() {
        when(exchange()).thenThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY));
        RestTemplateServiceImpl service = service(2);

        AnalysisApiException failure = captureFailure(service);

        assertThat(failure.getFailureType()).isEqualTo(FailureType.SERVER_ERROR);
        assertThat(failure.getUpstreamStatus()).isEqualTo(502);
        verify(restTemplate, times(2)).exchange(
                eq(API_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }

    @Test
    public void upstreamClientErrorIsNotRetried() {
        when(exchange()).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));
        RestTemplateServiceImpl service = service(3);

        AnalysisApiException failure = captureFailure(service);

        assertThat(failure.getFailureType()).isEqualTo(FailureType.RESPONSE_ERROR);
        assertThat(failure.getUpstreamStatus()).isEqualTo(400);
        assertThat(failure.isRetryable()).isFalse();
        verify(restTemplate).exchange(
                eq(API_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }

    @Test
    public void emptySuccessfulResponseIsRejectedWithoutRetry() {
        when(exchange()).thenReturn(ok("  "));
        RestTemplateServiceImpl service = service(3);

        AnalysisApiException failure = captureFailure(service);

        assertThat(failure.getFailureType()).isEqualTo(FailureType.RESPONSE_ERROR);
        assertThat(failure.isRetryable()).isFalse();
        verify(restTemplate).exchange(
                eq(API_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }

    @Test
    public void malformedSuccessfulResponseIsRejectedWithoutRetry() {
        when(exchange()).thenReturn(ok("not-a-result-id"));
        RestTemplateServiceImpl service = service(3);

        AnalysisApiException failure = captureFailure(service);

        assertThat(failure.getFailureType()).isEqualTo(FailureType.RESPONSE_ERROR);
        assertThat(failure.isRetryable()).isFalse();
        verify(restTemplate).exchange(
                eq(API_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }

    @Test
    public void nonPositiveSuccessfulResponseIsRejectedWithoutRetry() {
        when(exchange()).thenReturn(ok("0"));
        RestTemplateServiceImpl service = service(3);

        AnalysisApiException failure = captureFailure(service);

        assertThat(failure.getFailureType()).isEqualTo(FailureType.RESPONSE_ERROR);
        verify(restTemplate).exchange(
                eq(API_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }

    @Test
    public void resultAboveConfiguredMappingRangeIsRejectedWithoutRetry() {
        when(exchange()).thenReturn(ok("45"));
        RestTemplateServiceImpl service = service(3);

        AnalysisApiException failure = captureFailure(service);

        assertThat(failure.getFailureType()).isEqualTo(FailureType.RESPONSE_ERROR);
        verify(restTemplate).exchange(
                eq(API_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }

    @Test
    public void rejectsBlankInputsAndUnboundedRetryConfiguration() {
        assertThatThrownBy(() -> service(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service(4)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service(1).callFlaskApi("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void catalogHashMismatchIsRejectedWithoutRetry() {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("X-RoadScanner-Catalog-SHA256",
                "0000000000000000000000000000000000000000000000000000000000000000");
        when(exchange()).thenReturn(new ResponseEntity<>("7", responseHeaders, HttpStatus.OK));

        AnalysisApiException failure = captureFailure(service(3));

        assertThat(failure.getFailureType()).isEqualTo(FailureType.RESPONSE_ERROR);
        verify(restTemplate).exchange(
                eq(API_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }

    private RestTemplateServiceImpl service(int maxAttempts) {
        return new RestTemplateServiceImpl(
                restTemplate, API_URL, maxAttempts, 44, CATALOG_SHA256);
    }

    private AnalysisApiException captureFailure(RestTemplateServiceImpl service) {
        Throwable thrown = catchThrowable(() -> service.callFlaskApi(IMAGE_URL));
        assertThat(thrown).isInstanceOf(AnalysisApiException.class);
        return (AnalysisApiException) thrown;
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<String> exchange() {
        return restTemplate.exchange(
                eq(API_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }

    private ResponseEntity<String> ok(String body) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("X-RoadScanner-Catalog-SHA256", CATALOG_SHA256);
        return new ResponseEntity<>(body, responseHeaders, HttpStatus.OK);
    }
}
