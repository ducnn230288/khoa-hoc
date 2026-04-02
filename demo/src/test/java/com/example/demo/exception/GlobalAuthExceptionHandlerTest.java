package com.example.demo.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import com.example.demo.audit.AuditLogger;
import com.example.demo.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

class GlobalAuthExceptionHandlerTest {

	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
	private final AuditLogger auditLogger = mock(AuditLogger.class);
	private final GlobalAuthExceptionHandler handler = new GlobalAuthExceptionHandler(this.objectMapper, this.auditLogger);

	@Test
	void commenceReturnsSessionRequiredWhenNoSessionExists() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/csrf");
		MockHttpServletResponse response = new MockHttpServletResponse();

		this.handler.commence(request, response, new InsufficientAuthenticationException("Auth required"));

		ErrorResponse body = this.objectMapper.readValue(response.getContentAsByteArray(), ErrorResponse.class);
		assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
		assertEnvelope(body, "AUTH_SESSION_REQUIRED", "/api/v1/auth/csrf");
		assertThat(response.getContentType()).isEqualTo("application/json");
		verify(this.auditLogger).logAccessDeniedUnauthenticated("/api/v1/auth/csrf");
	}

	@Test
	void commenceReturnsSessionExpiredWhenSessionIdIsInvalid() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/csrf");
		request.setRequestedSessionId("expired-session");
		request.setRequestedSessionIdValid(false);
		MockHttpServletResponse response = new MockHttpServletResponse();

		this.handler.commence(request, response, new InsufficientAuthenticationException("Expired"));

		ErrorResponse body = this.objectMapper.readValue(response.getContentAsByteArray(), ErrorResponse.class);
		assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
		assertEnvelope(body, "AUTH_SESSION_EXPIRED", "/api/v1/auth/csrf");
		verify(this.auditLogger).logAccessDeniedSessionExpired("/api/v1/auth/csrf");
	}

	@Test
	void handleReturnsCsrfEnvelope() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/logout");
		MockHttpServletResponse response = new MockHttpServletResponse();

		this.handler.handle(request, response, new AccessDeniedException("Forbidden"));

		ErrorResponse body = this.objectMapper.readValue(response.getContentAsByteArray(), ErrorResponse.class);
		assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
		assertEnvelope(body, "AUTH_CSRF_INVALID", "/api/v1/auth/logout");
		verify(this.auditLogger).logAccessDeniedCsrf("/api/v1/auth/logout");
	}

	@Test
	void badCredentialsMapsToInvalidCredentialsEnvelope() {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");

		var response = this.handler.handleBadCredentials(new BadCredentialsException("Bad credentials"), request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(response.getBody()).isNotNull();
		assertEnvelope(response.getBody(), "AUTH_INVALID_CREDENTIALS", "/api/v1/auth/login");
	}

	@Test
	void disabledUserMapsToDisabledEnvelope() {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");

		var response = this.handler.handleDisabled(new DisabledException("Disabled"), request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(response.getBody()).isNotNull();
		assertEnvelope(response.getBody(), "AUTH_USER_DISABLED", "/api/v1/auth/login");
	}

	private void assertEnvelope(ErrorResponse body, String code, String path) {
		assertThat(body).isNotNull();
		assertThat(body.code()).isEqualTo(code);
		assertThat(body.message()).isNotBlank();
		assertThat(body.path()).isEqualTo(path);
		assertThat(body.timestamp()).isNotNull();
	}
}
