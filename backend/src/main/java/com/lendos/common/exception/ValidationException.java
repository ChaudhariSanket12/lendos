package com.lendos.common.exception;

import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Map;

public class ValidationException extends BusinessException {

    private final Map<String, String> errors;

    public ValidationException(Map<String, String> errors) {
        super("VALIDATION_FAILED", "Validation failed", HttpStatus.BAD_REQUEST);
        this.errors = Collections.unmodifiableMap(errors);
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
