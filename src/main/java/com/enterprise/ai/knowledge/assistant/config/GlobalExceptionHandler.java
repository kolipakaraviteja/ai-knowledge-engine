package com.enterprise.ai.knowledge.assistant.config;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST API endpoints
 * Provides consistent error responses with proper HTTP status codes and structured logging
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String COMPONENT = "GLOBAL_EXCEPTION_HANDLER";
    private static final String ERROR_TYPE = "error_type";
    private static final String HTTP_STATUS = "http_status";
    private static final String ERROR_MESSAGE = "error_message";
    private static final String FIELD_COUNT = "field_count";

    /**
     * Handle validation errors from @Valid annotation
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        // Structured logging
        MDC.put("component", COMPONENT);
        MDC.put(ERROR_TYPE, "validation_error");
        MDC.put(HTTP_STATUS, String.valueOf(HttpStatus.BAD_REQUEST.value()));
        MDC.put(FIELD_COUNT, String.valueOf(errors.size()));
        
        log.warn("[{}] Validation error: field_count={}, errors={}", correlationId, errors.size(), errors);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Invalid request parameters")
                .details(errors)
                .build();
        
        clearMdc();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle file upload size exceeded
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        
        // Structured logging
        MDC.put("component", COMPONENT);
        MDC.put(ERROR_TYPE, "file_size_exceeded");
        MDC.put(HTTP_STATUS, String.valueOf(HttpStatus.PAYLOAD_TOO_LARGE.value()));
        MDC.put(ERROR_MESSAGE, ex.getMessage());
        
        log.warn("[{}] File upload size exceeded: message={}", correlationId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.PAYLOAD_TOO_LARGE.value())
                .error("File Too Large")
                .message("The uploaded file exceeds the maximum allowed size")
                .build();
        
        clearMdc();
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        
        // Structured logging
        MDC.put("component", COMPONENT);
        MDC.put(ERROR_TYPE, "illegal_argument");
        MDC.put(HTTP_STATUS, String.valueOf(HttpStatus.BAD_REQUEST.value()));
        MDC.put(ERROR_MESSAGE, ex.getMessage());
        
        log.warn("[{}] Illegal argument: message={}", correlationId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Invalid Argument")
                .message(ex.getMessage())
                .build();
        
        clearMdc();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle illegal state exceptions
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        
        // Structured logging
        MDC.put("component", COMPONENT);
        MDC.put(ERROR_TYPE, "illegal_state");
        MDC.put(HTTP_STATUS, String.valueOf(HttpStatus.CONFLICT.value()));
        MDC.put(ERROR_MESSAGE, ex.getMessage());
        
        log.warn("[{}] Illegal state: message={}", correlationId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Invalid State")
                .message(ex.getMessage())
                .build();
        
        clearMdc();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handle generic exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        
        // Structured logging
        MDC.put("component", COMPONENT);
        MDC.put(ERROR_TYPE, "generic_exception");
        MDC.put(HTTP_STATUS, String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()));
        MDC.put("exception_class", ex.getClass().getSimpleName());
        MDC.put(ERROR_MESSAGE, ex.getMessage());
        
        log.error("[{}] Unexpected error occurred: exception_class={}, message={}", 
                correlationId, ex.getClass().getSimpleName(), ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred. Please try again later.")
                .build();
        
        clearMdc();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Clear MDC after logging operations
     */
    private void clearMdc() {
        MDC.remove("component");
        MDC.remove(ERROR_TYPE);
        MDC.remove(HTTP_STATUS);
        MDC.remove(ERROR_MESSAGE);
        MDC.remove(FIELD_COUNT);
        MDC.remove("exception_class");
    }

    /**
     * Error response DTO
     */
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private Map<String, String> details;

        public static ErrorResponseBuilder builder() {
            return new ErrorResponseBuilder();
        }

        // Getters and Setters
        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Map<String, String> getDetails() {
            return details;
        }

        public void setDetails(Map<String, String> details) {
            this.details = details;
        }

        public static class ErrorResponseBuilder {
            private ErrorResponse response;

            private ErrorResponseBuilder() {
                response = new ErrorResponse();
            }

            public ErrorResponseBuilder timestamp(LocalDateTime timestamp) {
                response.timestamp = timestamp;
                return this;
            }

            public ErrorResponseBuilder status(int status) {
                response.status = status;
                return this;
            }

            public ErrorResponseBuilder error(String error) {
                response.error = error;
                return this;
            }

            public ErrorResponseBuilder message(String message) {
                response.message = message;
                return this;
            }

            public ErrorResponseBuilder details(Map<String, String> details) {
                response.details = details;
                return this;
            }

            public ErrorResponse build() {
                return response;
            }
        }
    }
}
