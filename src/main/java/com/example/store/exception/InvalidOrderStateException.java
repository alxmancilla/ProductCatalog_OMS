package com.example.store.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an operation is attempted on an order that is in an invalid state.
 * 
 * Examples:
 * - Trying to cancel an order that is already SHIPPED
 * - Trying to modify an order that is not in PENDING status
 * - Trying to ship an order that is CANCELLED
 * 
 * Returns HTTP 409 CONFLICT status.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class InvalidOrderStateException extends RuntimeException {

    public InvalidOrderStateException(String message) {
        super(message);
    }

    public InvalidOrderStateException(String message, Throwable cause) {
        super(message, cause);
    }
}

