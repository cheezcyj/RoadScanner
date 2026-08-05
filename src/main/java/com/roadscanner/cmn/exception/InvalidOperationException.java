package com.roadscanner.cmn.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidOperationException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    public InvalidOperationException(String message) {
        super(message);
    }
}
