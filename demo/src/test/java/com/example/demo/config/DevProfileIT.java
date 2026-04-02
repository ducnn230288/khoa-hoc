package com.example.demo.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@Testcontainers
class DevProfileIT {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
			.withDatabaseName("dev_profile_it")
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

	private final HttpClient client = HttpClient.newHttpClient();

	@Test
	void devExposesOpenApiAndIssuesSameSiteNoneCookiesWithoutSecureFlag() throws Exception {
		HttpResponse<String> apiDocsResponse = this.client.send(
				HttpRequest.newBuilder(URI.create("http://localhost:" + this.port + "/v3/api-docs"))
						.GET()
						.build(),
				HttpResponse.BodyHandlers.ofString());
		HttpResponse<String> loginResponse = this.client.send(
				HttpRequest.newBuilder(URI.create("http://localhost:" + this.port + "/api/v1/auth/login"))
						.header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.POST(HttpRequest.BodyPublishers.ofString(
								"{\"username\":\"user01\",\"password\":\"User@123\"}"))
						.build(),
				HttpResponse.BodyHandlers.ofString());

		assertThat(apiDocsResponse.statusCode()).isEqualTo(200);
		assertThat(apiDocsResponse.body()).contains("/api/v1/auth/login");
		assertThat(apiDocsResponse.body()).contains("username");
		assertThat(apiDocsResponse.body()).contains("X-CSRF-TOKEN");
		assertThat(apiDocsResponse.body()).doesNotContain("\"email\"");
		assertThat(loginResponse.headers().allValues("set-cookie")).anySatisfy(cookie -> assertThat(cookie)
				.contains("JSESSIONID=")
				.contains("SameSite=None")
				.doesNotContain("Secure"));
	}
}
