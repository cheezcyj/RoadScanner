package com.roadscanner.config;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClientException;

/**
 * Classifies upstream HTTP failures without buffering or exposing their body.
 */
public final class StatusOnlyResponseErrorHandler implements ResponseErrorHandler {

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return response.getRawStatusCode() >= 400;
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        int rawStatus = response.getRawStatusCode();
        HttpStatus status = HttpStatus.resolve(rawStatus);
        if (status != null && status.is4xxClientError()) {
            throw new HttpClientErrorException(status);
        }
        if (status != null && status.is5xxServerError()) {
            throw new HttpServerErrorException(status);
        }
        throw new RestClientException("Analysis service returned an error status");
    }
}
