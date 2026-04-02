package com.example.demo.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("staging")
@Testcontainers
class StagingProfileIT {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
			.withDatabaseName("staging_profile_it")
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
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private final HttpClient client = HttpClient.newHttpClient();

	@Test
	void stagingKeepsOpenApiPublicUsesSecureSessionCookieAndDoesNotLoadSeedUsers() throws Exception {
		Integer seedCount = this.jdbcTemplate.queryForObject(
				"select count(*) from users where username in ('admin', 'user01')", Integer.class);
		String username = "staging-" + UUID.randomUUID();
		this.jdbcTemplate.update("insert into users (username, password_hash, enabled) values (?, ?, ?)",
				username, this.passwordEncoder.encode("Staging@123"), true);

		HttpResponse<String> response = this.client.send(
				HttpRequest.newBuilder(URI.create("http://localhost:" + this.port + "/v3/api-docs"))
						.GET()
						.build(),
				HttpResponse.BodyHandlers.ofString());
		HttpResponse<String> loginResponse = this.client.send(
				HttpRequest.newBuilder(URI.create("http://localhost:" + this.port + "/api/v1/auth/login"))
						.header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.POST(HttpRequest.BodyPublishers.ofString(
								"{\"username\":\"" + username + "\",\"password\":\"Staging@123\"}"))
						.build(),
				HttpResponse.BodyHandlers.ofString());

		assertThat(seedCount).isZero();
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("/api/v1/auth/login");
		assertThat(loginResponse.headers().allValues("set-cookie")).anySatisfy(cookie -> assertThat(cookie)
				.contains("JSESSIONID=").contains("Secure"));
	}
}
