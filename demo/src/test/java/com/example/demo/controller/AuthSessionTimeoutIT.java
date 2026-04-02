package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.apache.catalina.Context;
import org.apache.catalina.Session;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.demo.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class AuthSessionTimeoutIT {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
			.withDatabaseName("auth_timeout_it")
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
	private ServletWebServerApplicationContext applicationContext;

	private final HttpClient client = HttpClient.newHttpClient();

	@Test
	void expiredSessionReturnsDedicatedEnvelope() throws Exception {
		HttpResponse<String> loginResponse = send("POST", "/api/v1/auth/login",
				"""
					{"username":"user01","password":"User@123"}
					""",
				MediaType.APPLICATION_JSON_VALUE, null, null);
		String sessionCookie = loginResponse.headers().allValues("set-cookie").stream()
				.filter(value -> value.startsWith("JSESSIONID="))
				.findFirst()
				.orElseThrow();
		String cookieHeader = sessionCookie.substring(0, sessionCookie.indexOf(';'));
		String sessionId = cookieHeader.substring("JSESSIONID=".length());

		expireSession(sessionId);

		HttpResponse<String> csrfResponse = send("GET", "/api/v1/auth/csrf", null, null, "Cookie", cookieHeader);
		ErrorResponse body = this.objectMapper.readValue(csrfResponse.body(), ErrorResponse.class);

		assertThat(csrfResponse.statusCode()).isEqualTo(401);
		assertThat(body.code()).isEqualTo("AUTH_SESSION_EXPIRED");
	}

	private void expireSession(String sessionId) {
		TomcatWebServer webServer = (TomcatWebServer) this.applicationContext.getWebServer();
		Context context = (Context) webServer.getTomcat().getHost().findChildren()[0];
		try {
			Session session = context.getManager().findSession(sessionId);
			assertThat(session).isNotNull();
			session.expire();
		}
		catch (java.io.IOException ex) {
			throw new IllegalStateException("Failed to expire test session.", ex);
		}
	}

	private HttpResponse<String> send(String method, String path, String body, String contentType, String headerName,
			String headerValue) throws Exception {
		HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create("http://localhost:" + this.port + path));
		if (contentType != null) {
			builder.header("Content-Type", contentType);
		}
		if (headerName != null && headerValue != null) {
			builder.header(headerName, headerValue);
		}

		builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
		return this.client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}
}
