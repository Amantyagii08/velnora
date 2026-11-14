
package com.ruralmarketplace.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import com.ruralmarketplace.exception.LoginRateLimitException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFound(ResourceNotFoundException ex) {
        Map<String, String> error = new HashMap<>(); error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthentication(AuthenticationException ex) {
        Map<String, String> error = new HashMap<>();
        if (ex instanceof DisabledException) {
            error.put("message", "This account is suspended or disabled");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }
        error.put("message", "Invalid email or password");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(LoginRateLimitException.class)
    public ResponseEntity<Map<String, String>> handleLoginRateLimit(LoginRateLimitException ex) {
        Map<String, String> error = new HashMap<>(); error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", "You do not have permission to perform this action");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", "A record with one of these unique values already exists");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }


@ExceptionHandler(ShopNotFoundException.class)
public ResponseEntity<Map<String, String>> handleShopNotFound(
        ShopNotFoundException ex) {

    Map<String, String> error = new HashMap<>();
    error.put("message", ex.getMessage());

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
}

@ExceptionHandler(CategoryNotFoundException.class)
public ResponseEntity<Map<String, String>> handleCategoryNotFound(
        CategoryNotFoundException ex) {

    Map<String, String> error = new HashMap<>();
    error.put("message", ex.getMessage());

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
}


@ExceptionHandler(ProductNotFoundException.class)
public ResponseEntity<Map<String, String>> handleProductNotFound(
        ProductNotFoundException ex) {

    Map<String, String> error = new HashMap<>();
    error.put("message", ex.getMessage());

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
}


@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<Map<String, String>> handleIllegalArgument(
        IllegalArgumentException ex) {

    Map<String, String> error = new HashMap<>();
    error.put("message", ex.getMessage());

    return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
}


}

