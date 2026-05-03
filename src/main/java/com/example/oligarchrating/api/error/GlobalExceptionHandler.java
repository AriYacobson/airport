package com.example.oligarchrating.api.error;

import com.example.oligarchrating.exception.ExternalServiceException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

import java.time.Clock;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                     HttpServletRequest request) {
        List<ApiError.FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError.FieldViolation(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, violations);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleNotReadable(HttpMessageNotReadableException ex,
                                                      HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Malformed request body", request, List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex,
                                                          HttpServletRequest request) {
        log.warn("Illegal argument", ex);
        return build(HttpStatus.BAD_REQUEST, "Invalid request", request, List.of());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(OptimisticLockingFailureException ex,
                                                         HttpServletRequest request) {
        log.warn("Optimistic lock failure", ex);
        return build(HttpStatus.CONFLICT, "Concurrent update detected, please retry", request, List.of());
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ApiError> handleCircuitBreakerOpen(CallNotPermittedException ex,
                                                             HttpServletRequest request) {
        String breaker = ex.getCausingCircuitBreakerName();
        log.warn("Circuit breaker open for {}", breaker);
        return build(HttpStatus.SERVICE_UNAVAILABLE, upstreamUnavailable(breaker), request, List.of());
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiError> handleExternalService(ExternalServiceException ex,
                                                          HttpServletRequest request) {
        log.error("External service '{}' failed: {}", ex.getService(), ex.getMessage(), ex);
        HttpStatus status = ex.getCause() instanceof ResourceAccessException
                ? HttpStatus.GATEWAY_TIMEOUT
                : HttpStatus.BAD_GATEWAY;
        return build(status, upstreamUnavailable(ex.getService()), request, List.of());
    }

    private static String upstreamUnavailable(String service) {
        return "Upstream service '" + service + "' is unavailable";
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnknown(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", request, List.of());
    }

    private ResponseEntity<ApiError> build(HttpStatus status,
                                           String message,
                                           HttpServletRequest request,
                                           List<ApiError.FieldViolation> violations) {
        ApiError body = new ApiError(
                clock.instant(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                violations);
        return ResponseEntity.status(status).body(body);
    }
}
