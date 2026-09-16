package com.volako.backend.exception;

import org.springframework.http.HttpStatus;

/** Raised when a request is well-formed but violates a business rule (HTTP 409). */
public class BusinessConflictException extends ApiException {

    public BusinessConflictException(String errorCode, String message) {
        super(HttpStatus.CONFLICT, errorCode, message);
    }
}
