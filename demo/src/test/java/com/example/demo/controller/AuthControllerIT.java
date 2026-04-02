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
		assertThat(body.code()).isEqualTo("AUTH_INVALID_CREDENTIALS");
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
		assertThat(body.code()).isEqualTo("AUTH_USER_DISABLED");
	}

	@Test
	void csrfEndpointRequiresSessionAndSupportsRehydrateWithCookie() throws Exception {
		HttpResponse<String> noSessionResponse = send("GET", "/api/v1/auth/csrf", null, Map.of());
		ErrorResponse noSessionBody = this.objectMapper.readValue(noSessionResponse.body(), ErrorResponse.class);
		assertThat(noSessionResponse.statusCode()).isEqualTo(401);
		assertThat(noSessionBody.code()).isEqualTo("AUTH_SESSION_REQUIRED");

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
		assertThat(noSessionBody.code()).isEqualTo("AUTH_SESSION_REQUIRED");

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
		assertThat(missingCsrfBody.code()).isEqualTo("AUTH_CSRF_INVALID");

		HttpResponse<String> invalidCsrfResponse = send("POST", "/api/v1/auth/logout", "",
				Map.of("Cookie", sessionCookie, "X-CSRF-TOKEN", "not-valid"));
		ErrorResponse invalidCsrfBody = this.objectMapper.readValue(invalidCsrfResponse.body(), ErrorResponse.class);
		assertThat(invalidCsrfResponse.statusCode()).isEqualTo(403);
		assertThat(invalidCsrfBody.code()).isEqualTo("AUTH_CSRF_INVALID");
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
				"Access-Control-Request-Headers", "content-type"));

		assertThat(response.statusCode()).isBetween(200, 204);
		assertThat(response.headers().firstValue("access-control-allow-origin")).hasValue("http://localhost:5173");
		assertThat(response.headers().firstValue("access-control-allow-credentials")).hasValue("true");
		assertThat(response.headers().firstValue("access-control-allow-methods"))
				.hasValueSatisfying(value -> assertThat(value).contains("POST"));
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
