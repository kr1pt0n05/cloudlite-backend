package de.lind3.CloudLite.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Translates exceptions thrown by any controller or service into a consistent
 * {@link ErrorResponse} JSON body so that clients always receive a structured
 * error message rather than a default empty or HTML error page.
 *
 * <p>Handled exception types:
 * <ul>
 *   <li>{@link ResponseStatusException} – the status and reason message are
 *       forwarded directly to the client.</li>
 *   <li>{@link UnsupportedOperationException} – returned as 501 Not Implemented.</li>
 *   <li>{@link IllegalStateException} – returned as 500 Internal Server Error
 *       with a generic message to avoid leaking internal details.</li>
 *   <li>Any other {@link Exception} – fallback 500 with a generic message.</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles all {@link ResponseStatusException} instances.
     * The HTTP status and reason string set by the thrower are forwarded as-is,
     * so the client sees the exact message the service layer intended.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        String error = status != null ? status.getReasonPhrase() : "Error";
        String message = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(new ErrorResponse(ex.getStatusCode().value(), error, message));
    }

    /**
     * Handles {@link UnsupportedOperationException}, typically thrown by service
     * methods that are not yet implemented.
     */
    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedOperation(UnsupportedOperationException ex) {
        HttpStatus status = HttpStatus.NOT_IMPLEMENTED;
        String message = ex.getMessage() != null ? ex.getMessage() : "This operation is not yet implemented";
        return ResponseEntity
                .status(status)
                .body(new ErrorResponse(status.value(), status.getReasonPhrase(), message));
    }

    /**
     * Handles {@link IllegalStateException}.
     * A generic message is returned to avoid leaking internal implementation details;
     * the original exception should be logged by the application's logging framework.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(status)
                .body(new ErrorResponse(status.value(), status.getReasonPhrase(),
                        "An internal server error occurred"));
    }

    /**
     * Fallback handler for any unhandled exception.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(status)
                .body(new ErrorResponse(status.value(), status.getReasonPhrase(),
                        "An unexpected error occurred"));
    }
}
