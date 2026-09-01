package com.computershare.regfiling.web;

import com.computershare.regfiling.web.dto.ApiExceptions;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiExceptions.NotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(ApiExceptions.NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ApiExceptions.ForbiddenException.class)
    public ResponseEntity<Map<String, String>> forbidden(ApiExceptions.ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ApiExceptions.ConflictException.class)
    public ResponseEntity<Map<String, String>> conflict(ApiExceptions.ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ex.getMessage()));
    }

    // openapi.yaml ValidationErrorResponse — FR-004/FR-010.
    @ExceptionHandler(ApiExceptions.ValidationFailedException.class)
    public ResponseEntity<Map<String, Object>> validationFailed(ApiExceptions.ValidationFailedException ex) {
        List<Map<String, String>> errors = ex.getErrors().stream()
                .map(e -> {
                    int idx = e.indexOf(':');
                    String field = idx > 0 ? e.substring(0, idx) : "unknown";
                    String message = idx > 0 ? e.substring(idx + 1).trim() : e;
                    return Map.of("field", field, "message", message);
                })
                .toList();
        return ResponseEntity.unprocessableEntity().body(Map.of("filingId", ex.getFilingId(), "errors", errors));
    }
}
