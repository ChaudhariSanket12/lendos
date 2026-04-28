package com.lendos.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resource, String id) {
        super(
            "RESOURCE_NOT_FOUND",
            resource + " not found with id: " + id,
            HttpStatus.NOT_FOUND
        );
    }
}
