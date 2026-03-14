package com.example.demo.exception;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setType(URI.create("https://errors.example.com/auth/invalid-credentials"));
        problem.setTitle("Authentication Failed");
        problem.setDetail("Invalid username or password.");
        problem.setInstance(URI.create(getRequestUri(request)));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setType(URI.create("https://errors.example.com/auth/access-denied"));
        problem.setTitle("Access Denied");
        problem.setDetail("You do not have permission to access this resource.");
        problem.setInstance(URI.create(getRequestUri(request)));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create("https://errors.example.com/validation/invalid-input"));
        problem.setTitle("Validation Failed");
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Invalid input.");
        problem.setDetail(detail);
        problem.setInstance(URI.create(getRequestUri(request)));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    private String getRequestUri(WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest) {
            return servletRequest.getRequest().getRequestURI();
        }
        return "/unknown";
    }
}
