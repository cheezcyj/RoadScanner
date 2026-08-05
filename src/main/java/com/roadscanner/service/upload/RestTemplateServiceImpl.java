package com.roadscanner.service.upload;

import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@Profile({"!local", "local-ml"})
public class RestTemplateServiceImpl implements RestTemplateService {

    private static final int MAX_ALLOWED_ATTEMPTS = 3;
    private static final int MAX_CONFIGURABLE_RESULT_ID = 10000;
    private static final String CATALOG_HASH_HEADER = "X-RoadScanner-Catalog-SHA256";

    private final String apiUrl;
    private final RestTemplate restTemplate;
    private final int maxAttempts;
    private final int maxResultId;
    private final String expectedCatalogSha256;

    @Autowired
    public RestTemplateServiceImpl(
            RestTemplate restTemplate,
            @Value("${roadscanner.flask.api-url}") String apiUrl,
            @Value("${roadscanner.flask.max-attempts}") int maxAttempts,
            @Value("${roadscanner.flask.result-id-max}") int maxResultId,
            @Value("${roadscanner.ml.catalog-sha256}") String expectedCatalogSha256) {
        if (restTemplate == null) {
            throw new IllegalArgumentException("restTemplate must not be null");
        }
        if (apiUrl == null || apiUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Analysis API URL must not be blank");
        }
        if (maxAttempts < 1 || maxAttempts > MAX_ALLOWED_ATTEMPTS) {
            throw new IllegalArgumentException("Analysis API max attempts must be between 1 and 3");
        }
        if (maxResultId < 1 || maxResultId > MAX_CONFIGURABLE_RESULT_ID) {
            throw new IllegalArgumentException("Analysis result ID upper bound is invalid");
        }
        if (expectedCatalogSha256 == null
                || !expectedCatalogSha256.matches("(?i)[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Analysis catalog SHA-256 is invalid");
        }
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
        this.maxAttempts = maxAttempts;
        this.maxResultId = maxResultId;
        this.expectedCatalogSha256 = expectedCatalogSha256.toUpperCase();
    }

    @Override
    public String callFlaskApi(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Image URL must not be blank");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.TEXT_PLAIN));

        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("image_url", imageUrl);
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestMap, headers);

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        apiUrl, HttpMethod.POST, requestEntity, String.class);
                String catalogHash = response.getHeaders().getFirst(CATALOG_HASH_HEADER);
                if (catalogHash == null
                        || !expectedCatalogSha256.equals(catalogHash.trim().toUpperCase())) {
                    throw AnalysisApiException.responseError(
                            response.getStatusCodeValue(), null);
                }
                String responseBody = response.getBody();
                if (responseBody == null || responseBody.trim().isEmpty()) {
                    throw AnalysisApiException.responseError(
                            response.getStatusCodeValue(), null);
                }
                String normalizedResponse = responseBody.trim();
                try {
                    int resultId = Integer.parseInt(normalizedResponse);
                    if (resultId <= 0 || resultId > maxResultId) {
                        throw AnalysisApiException.responseError(
                                response.getStatusCodeValue(), null);
                    }
                } catch (NumberFormatException invalidResponse) {
                    throw AnalysisApiException.responseError(
                            response.getStatusCodeValue(), invalidResponse);
                }
                return normalizedResponse;
            } catch (RestClientException exception) {
                AnalysisApiException failure = classify(exception);
                if (!failure.isRetryable() || attempt == maxAttempts) {
                    throw failure;
                }
            }
        }

        throw new IllegalStateException("Analysis API retry loop completed unexpectedly");
    }

    private AnalysisApiException classify(RestClientException exception) {
        if (exception instanceof HttpServerErrorException) {
            HttpServerErrorException serverError = (HttpServerErrorException) exception;
            return AnalysisApiException.serverError(serverError.getRawStatusCode(), serverError);
        }
        if (exception instanceof ResourceAccessException) {
            if (hasCause(exception, SocketTimeoutException.class)) {
                return AnalysisApiException.timeout(exception);
            }
            return AnalysisApiException.connection(exception);
        }
        if (exception instanceof HttpStatusCodeException) {
            HttpStatusCodeException responseError = (HttpStatusCodeException) exception;
            return AnalysisApiException.responseError(responseError.getRawStatusCode(), responseError);
        }
        return AnalysisApiException.responseError(0, exception);
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> causeType) {
        Throwable current = throwable;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
