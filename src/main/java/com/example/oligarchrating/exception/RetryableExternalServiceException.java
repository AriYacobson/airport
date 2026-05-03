package com.example.oligarchrating.exception;

/**
 * Marker for upstream failures that are safe to retry: network errors and 5xx responses.
 * 4xx responses are reported as the parent {@link ExternalServiceException} and will not be retried.
 */
public class RetryableExternalServiceException extends ExternalServiceException {

    public RetryableExternalServiceException(String service, String message, Throwable cause) {
        super(service, message, cause);
    }
}
