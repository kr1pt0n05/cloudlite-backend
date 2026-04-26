package de.lind3.CloudLite.common;

/**
 * Standardised error response body returned by {@link GlobalExceptionHandler}.
 *
 * @param status  HTTP status code (e.g. 404)
 * @param error   HTTP reason phrase (e.g. "Not Found")
 * @param message Human-readable description of what went wrong
 */
public record ErrorResponse(int status, String error, String message) {}
