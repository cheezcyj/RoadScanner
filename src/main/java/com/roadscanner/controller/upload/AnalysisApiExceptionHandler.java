package com.roadscanner.controller.upload;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.roadscanner.service.upload.AnalysisApiException;
import com.roadscanner.service.upload.AnalysisApiException.FailureType;

@ControllerAdvice(assignableTypes = UploadController.class)
public class AnalysisApiExceptionHandler {

    @ExceptionHandler(AnalysisApiException.class)
    public ResponseEntity<Void> handleAnalysisApiFailure(AnalysisApiException exception) {
        HttpStatus status = exception.getFailureType() == FailureType.TIMEOUT
                ? HttpStatus.GATEWAY_TIMEOUT
                : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).build();
    }
}
