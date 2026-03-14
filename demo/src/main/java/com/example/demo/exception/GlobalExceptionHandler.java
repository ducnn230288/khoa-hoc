package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Global exception handler — RFC 7807 Problem Details for all error responses.
 * SEC-9: Stack traces are never exposed in responses.
 * AR-B6: Never catch-and-swallow.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        // AC-6: login failure → 401 RFC 7807
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        pd.setType(URI.create("https://errors.example.com/auth/invalid-credentials"));
        pd.setTitle("Authentication Failed");
        pd.setDetail("Invalid username or password.");
        // instance is set by the controller if available; left null here
        return pd;
    }

    @ExceptionHandler(DisabledException.class)
    public ProblemDetail handleDisabled(DisabledException ex) {
        // AC-7: disabled user → 401 RFC 7807
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        pd.setType(URI.create("https://errors.example.com/auth/account-disabled"));
        pd.setTitle("Account Disabled");
        pd.setDetail("This account is disabled.");
        return pd;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        pd.setType(URI.create("https://errors.example.com/auth/session-required"));
        pd.setTitle("Unauthorized");
        pd.setDetail("Authentication is required to access this resource.");
        return pd;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        // AC-13/14: CSRF missing/invalid → 403 RFC 7807
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        pd.setType(URI.create("https://errors.example.com/access/forbidden"));
        pd.setTitle("Forbidden");
        pd.setDetail("Access denied.");
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        // AC-32: Bean validation failure → 400 RFC 7807
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed.");
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create("https://errors.example.com/validation/invalid-input"));
        pd.setTitle("Validation Failed");
        pd.setDetail(detail);
        return pd;
    }
}
