package de.lind3.CloudLite.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Development-only exception handler that exposes full exception details in API responses.
 */
@Profile("development")
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DevelopmentExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<DevelopmentErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        HttpStatusCode statusCode = resolveStatusCode(ex);
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        String error = status != null ? status.getReasonPhrase() : "Error";

        return ResponseEntity
                .status(statusCode)
                .body(new DevelopmentErrorResponse(
                        Instant.now(),
                        statusCode.value(),
                        error,
                        resolveMessage(ex),
                        ex.getClass().getName(),
                        request.getMethod(),
                        request.getRequestURI(),
                        causes(ex),
                        stackTrace(ex)));
    }

    private static HttpStatusCode resolveStatusCode(Exception ex) {
        if (ex instanceof ErrorResponseException errorResponseException) {
            return errorResponseException.getStatusCode();
        }
        if (ex instanceof ResponseStatusException responseStatusException) {
            return responseStatusException.getStatusCode();
        }
        if (ex instanceof UnsupportedOperationException) {
            return HttpStatus.NOT_IMPLEMENTED;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private static String resolveMessage(Exception ex) {
        if (ex instanceof ResponseStatusException responseStatusException
                && responseStatusException.getReason() != null) {
            return responseStatusException.getReason();
        }
        return ex.getMessage();
    }

    private static List<DevelopmentErrorCause> causes(Throwable throwable) {
        List<DevelopmentErrorCause> causes = new ArrayList<>();
        Throwable cause = throwable.getCause();
        while (cause != null) {
            causes.add(new DevelopmentErrorCause(
                    cause.getClass().getName(),
                    cause.getMessage(),
                    stackTrace(cause)));
            cause = cause.getCause();
        }
        return causes;
    }

    private static List<String> stackTrace(Throwable throwable) {
        return Arrays.stream(throwable.getStackTrace())
                .map(StackTraceElement::toString)
                .toList();
    }

    public record DevelopmentErrorResponse(
            Instant timestamp,
            int status,
            String error,
            String message,
            String exception,
            String method,
            String path,
            List<DevelopmentErrorCause> causes,
            List<String> stackTrace) {
    }

    public record DevelopmentErrorCause(String exception, String message, List<String> stackTrace) {
    }
}
