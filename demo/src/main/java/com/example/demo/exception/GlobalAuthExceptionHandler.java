package com.example.demo.exception;

import java.io.IOException;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.example.demo.audit.AuditLogger;
import com.example.demo.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@ControllerAdvice
public class GlobalAuthExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

	private final ObjectMapper objectMapper;
	private final AuditLogger auditLogger;

	public GlobalAuthExceptionHandler(ObjectMapper objectMapper, AuditLogger auditLogger) {
		this.objectMapper = objectMapper;
		this.auditLogger = auditLogger;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		ErrorResponse errorResponse;
		if (isExpiredSession(request)) {
			this.auditLogger.logAccessDeniedSessionExpired(request.getRequestURI());
			errorResponse = buildError("AUTH_SESSION_EXPIRED", "Session expired.", request.getRequestURI());
		}
		else {
			this.auditLogger.logAccessDeniedUnauthenticated(request.getRequestURI());
			errorResponse = buildError("AUTH_SESSION_REQUIRED", "Authentication is required.", request.getRequestURI());
		}

		writeResponse(response, HttpStatus.UNAUTHORIZED, errorResponse);
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		this.auditLogger.logAccessDeniedCsrf(request.getRequestURI());
		writeResponse(response, HttpStatus.FORBIDDEN,
				buildError("AUTH_CSRF_INVALID", "CSRF token is missing or invalid.", request.getRequestURI()));
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(buildError("AUTH_INVALID_CREDENTIALS", "Invalid username or password.", request.getRequestURI()));
	}

	@ExceptionHandler(DisabledException.class)
	public ResponseEntity<ErrorResponse> handleDisabled(DisabledException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(buildError("AUTH_USER_DISABLED", "User account is disabled.", request.getRequestURI()));
	}

	private boolean isExpiredSession(HttpServletRequest request) {
		return request.getRequestedSessionId() != null && !request.isRequestedSessionIdValid();
	}

	private ErrorResponse buildError(String code, String message, String path) {
		return new ErrorResponse(code, message, path, Instant.now());
	}

	private void writeResponse(HttpServletResponse response, HttpStatus status, ErrorResponse body) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		this.objectMapper.writeValue(response.getOutputStream(), body);
	}
}
