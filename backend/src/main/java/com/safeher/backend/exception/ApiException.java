package com.safeher.backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Thrown when a request fails for a reason worth explaining to the user. */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
