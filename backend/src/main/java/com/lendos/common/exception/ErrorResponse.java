package com.lendos.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final String correlationId;
    private final String errorCode;
    private final String message;
    private final int status;
    private final LocalDateTime timestamp;
    private final Map<String, String> errors;
    private final List<FieldError> fieldErrors;

    @Getter
    @Builder
    public static class FieldError {
        private final String field;
        private final String message;
        private final Object rejectedValue;
    }
}
