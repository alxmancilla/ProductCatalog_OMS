package com.example.store.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for validation errors.
 * Provides user-friendly error messages when validation fails.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle validation errors for @Valid annotated request bodies.
     * Returns a map of field names to error messages.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Validation failed");
        response.put("errors", errors);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle insufficient inventory errors.
     * Returns details about which products don't have enough stock.
     */
    @ExceptionHandler(InsufficientInventoryException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientInventoryException(
            InsufficientInventoryException ex) {

        Map<String, Object> inventoryDetails = new HashMap<>();
        ex.getInsufficientProducts().forEach((productId, info) -> {
            Map<String, Object> details = new HashMap<>();
            details.put("productName", info.getProductName());
            details.put("requested", info.getRequested());
            details.put("available", info.getAvailable());
            inventoryDetails.put(productId, details);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Insufficient inventory for one or more products");
        response.put("insufficientProducts", inventoryDetails);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle product not found errors.
     */
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleProductNotFoundException(
            ProductNotFoundException ex) {

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());
        response.put("productId", ex.getProductId());

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
}

