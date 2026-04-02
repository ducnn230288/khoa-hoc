package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.demo.dto.CsrfResponse;
import com.example.demo.dto.ErrorResponse;
import com.example.demo.dto.LoginResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class AuthControllerIT {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
			.withDatabaseName("auth_controller_it")
			.withUsername("demo")
			.withPassword("demo");

	@DynamicPropertySource
	static void postgresProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@LocalServerPort
	private int port;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private final HttpClient client = HttpClient.newHttpClient();

	@Test
	void loginSuccessReturnsSessionCookieAndRoles() throws Exception {
		HttpResponse<String> response = sendJson("POST", "/api/v1/auth/login",
				"""
					{"username":"user01","password":"User@123"}
					""",
				Map.of());

		LoginResponse body = this.objectMapper.readValue(response.body(), LoginResponse.class);

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(body.authenticated()).isTrue();
		assertThat(body.username()).isEqualTo("user01");
		assertThat(body.roles()).containsExactly("USER");
		assertThat(sessionCookie(response)).contains("JSESSIONID=").contains("HttpOnly").contains("Path=/")
				.contains("SameSite=None").doesNotContain("Secure");
	}

	@Test
	void loginWithInvalidCredentialsReturns401EnvelopeWithoutCookie() throws Exception {
		HttpResponse<String> response = sendJson("POST", "/api/v1/auth/login",
				"""
					{"username":"user01","password":"wrong-password"}
					""",
				Map.of());

		ErrorResponse body = this.objectMapper.readValue(response.body(), ErrorResponse.class);

		assertThat(response.statusCode()).isEqualTo(401);
		assertErrorEnvelope(body, "AUTH_INVALID_CREDENTIALS", "/api/v1/auth/login");
		assertThat(response.headers().allValues("set-cookie")).isEmpty();
	}

	@Test
	void disabledUsersReturnDedicatedEnvelope() throws Exception {
		String username = "disabled-" + UUID.randomUUID();
		this.jdbcTemplate.update("insert into users (username, password_hash, enabled) values (?, ?, ?)",
				username, this.passwordEncoder.encode("Disabled@123"), false);

		HttpResponse<String> response = sendJson("POST", "/api/v1/auth/login",
				"{\"username\":\"" + username + "\",\"password\":\"Disabled@123\"}", Map.of());

		ErrorResponse body = this.objectMapper.readValue(response.body(), ErrorResponse.class);

		assertThat(response.statusCode()).isEqualTo(401);
		assertErrorEnvelope(body, "AUTH_USER_DISABLED", "/api/v1/auth/login");
	}

	@Test
	void csrfEndpointRequiresSessionAndSupportsRehydrateWithCookie() throws Exception {
		HttpResponse<String> noSessionResponse = send("GET", "/api/v1/auth/csrf", null, Map.of());
		ErrorResponse noSessionBody = this.objectMapper.readValue(noSessionResponse.body(), ErrorResponse.class);
		assertThat(noSessionResponse.statusCode()).isEqualTo(401);
		assertErrorEnvelope(noSessionBody, "AUTH_SESSION_REQUIRED", "/api/v1/auth/csrf");

		HttpResponse<String> loginResponse = sendJson("POST", "/api/v1/auth/login",
				"""
					{"username":"user01","password":"User@123"}
					""",
				Map.of());
		String sessionCookie = sessionCookieValue(loginResponse);

		HttpResponse<String> csrfResponse = send("GET", "/api/v1/auth/csrf", null, Map.of("Cookie", sessionCookie));
		CsrfResponse csrfBody = this.objectMapper.readValue(csrfResponse.body(), CsrfResponse.class);

		assertThat(csrfResponse.statusCode()).isEqualTo(200);
		assertThat(csrfBody.headerName()).isEqualTo("X-CSRF-TOKEN");
		assertThat(csrfBody.parameterName()).isEqualTo("_csrf");
		assertThat(csrfBody.csrfToken()).isNotBlank();
	}

	@Test
	void logoutRequiresSessionAndValidCsrf() throws Exception {
		HttpResponse<String> noSessionResponse = send("POST", "/api/v1/auth/logout", "", Map.of());
		ErrorResponse noSessionBody = this.objectMapper.readValue(noSessionResponse.body(), ErrorResponse.class);
		assertThat(noSessionResponse.statusCode()).isEqualTo(401);
		assertErrorEnvelope(noSessionBody, "AUTH_SESSION_REQUIRED", "/api/v1/auth/logout");

		HttpResponse<String> loginResponse = sendJson("POST", "/api/v1/auth/login",
				"""
					{"username":"user01","password":"User@123"}
					""",
				Map.of());
		String sessionCookie = sessionCookieValue(loginResponse);

		HttpResponse<String> missingCsrfResponse = send("POST", "/api/v1/auth/logout", "",
				Map.of("Cookie", sessionCookie));
		ErrorResponse missingCsrfBody = this.objectMapper.readValue(missingCsrfResponse.body(), ErrorResponse.class);
		assertThat(missingCsrfResponse.statusCode()).isEqualTo(403);
		assertErrorEnvelope(missingCsrfBody, "AUTH_CSRF_INVALID", "/api/v1/auth/logout");

		HttpResponse<String> invalidCsrfResponse = send("POST", "/api/v1/auth/logout", "",
				Map.of("Cookie", sessionCookie, "X-CSRF-TOKEN", "not-valid"));
		ErrorResponse invalidCsrfBody = this.objectMapper.readValue(invalidCsrfResponse.body(), ErrorResponse.class);
		assertThat(invalidCsrfResponse.statusCode()).isEqualTo(403);
		assertErrorEnvelope(invalidCsrfBody, "AUTH_CSRF_INVALID", "/api/v1/auth/logout");
	}

	@Test
	void logoutSuccessClearsCookie() throws Exception {
		HttpResponse<String> loginResponse = sendJson("POST", "/api/v1/auth/login",
				"""
					{"username":"user01","password":"User@123"}
					""",
				Map.of());
		String sessionCookie = sessionCookieValue(loginResponse);

		HttpResponse<String> csrfResponse = send("GET", "/api/v1/auth/csrf", null, Map.of("Cookie", sessionCookie));
		CsrfResponse csrfBody = this.objectMapper.readValue(csrfResponse.body(), CsrfResponse.class);

		HttpResponse<String> logoutResponse = send("POST", "/api/v1/auth/logout", "",
				Map.of("Cookie", sessionCookie, "X-CSRF-TOKEN", csrfBody.csrfToken()));

		assertThat(logoutResponse.statusCode()).isEqualTo(200);
		assertThat(logoutResponse.body()).contains("\"success\":true");
		assertThat(sessionCookie(logoutResponse)).contains("Max-Age=0").contains("HttpOnly").contains("Path=/")
				.contains("SameSite=None");

		HttpResponse<String> postLogoutCsrfResponse = send("GET", "/api/v1/auth/csrf", null, Map.of("Cookie", sessionCookie));
		ErrorResponse postLogoutCsrfBody = this.objectMapper.readValue(postLogoutCsrfResponse.body(), ErrorResponse.class);
		assertThat(postLogoutCsrfResponse.statusCode()).isEqualTo(401);
		assertThat(postLogoutCsrfBody.code()).isIn("AUTH_SESSION_REQUIRED", "AUTH_SESSION_EXPIRED");
		assertThat(postLogoutCsrfBody.path()).isEqualTo("/api/v1/auth/csrf");
		assertThat(postLogoutCsrfBody.timestamp()).isNotNull();
	}

	@Test
	void loginResponseKeepsRolesAsArrayWhenUserHasNoRoles() throws Exception {
		String username = "norole-" + UUID.randomUUID();
		this.jdbcTemplate.update("insert into users (username, password_hash, enabled) values (?, ?, ?)",
				username, this.passwordEncoder.encode("NoRole@123"), true);

		HttpResponse<String> response = sendJson("POST", "/api/v1/auth/login",
				"{\"username\":\"" + username + "\",\"password\":\"NoRole@123\"}", Map.of());
		LoginResponse body = this.objectMapper.readValue(response.body(), LoginResponse.class);

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(body.roles()).isEmpty();
	}

	@Test
	void corsPreflightAllowsConfiguredOriginAndCredentials() throws Exception {
		HttpResponse<String> response = send("OPTIONS", "/api/v1/auth/login", null, Map.of(
				"Origin", "http://localhost:5173",
				"Access-Control-Request-Method", "POST",
				"Access-Control-Request-Headers", "content-type,x-csrf-token"));

		assertThat(response.statusCode()).isBetween(200, 204);
		assertThat(response.headers().firstValue("access-control-allow-origin")).hasValue("http://localhost:5173");
		assertThat(response.headers().firstValue("access-control-allow-origin"))
				.hasValueSatisfying(value -> assertThat(value).isNotEqualTo("*"));
		assertThat(response.headers().firstValue("access-control-allow-credentials")).hasValue("true");
		assertThat(response.headers().firstValue("access-control-allow-methods"))
				.hasValueSatisfying(value -> assertThat(value).contains("POST"));
		assertThat(response.headers().firstValue("access-control-allow-headers"))
				.hasValueSatisfying(value -> assertThat(value).containsIgnoringCase("content-type")
						.containsIgnoringCase("x-csrf-token"));
	}

	private void assertErrorEnvelope(ErrorResponse body, String code, String path) {
		assertThat(body.code()).isEqualTo(code);
		assertThat(body.message()).isNotBlank();
		assertThat(body.path()).isEqualTo(path);
		assertThat(body.timestamp()).isNotNull();
	}

	private HttpResponse<String> sendJson(String method, String path, String body, Map<String, String> headers)
			throws IOException, InterruptedException {
		return send(method, path, body, merge(headers, Map.of("Content-Type", MediaType.APPLICATION_JSON_VALUE)));
	}

	private HttpResponse<String> send(String method, String path, String body, Map<String, String> headers)
			throws IOException, InterruptedException {
		HttpRequest.BodyPublisher publisher = body == null
				? HttpRequest.BodyPublishers.noBody()
				: HttpRequest.BodyPublishers.ofString(body);

		HttpRequest.Builder builder = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:" + this.port + path))
				.method(method, publisher);

		headers.forEach(builder::header);
		return this.client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	private Map<String, String> merge(Map<String, String> first, Map<String, String> second) {
		java.util.LinkedHashMap<String, String> merged = new java.util.LinkedHashMap<>(first);
		merged.putAll(second);
		return merged;
	}

	private String sessionCookie(HttpResponse<String> response) {
		return response.headers().allValues("set-cookie").stream()
				.filter(value -> value.startsWith("JSESSIONID="))
				.findFirst()
				.orElseThrow();
	}

	private String sessionCookieValue(HttpResponse<String> response) {
		String cookie = sessionCookie(response);
		return cookie.substring(0, cookie.indexOf(';'));
	}
}
