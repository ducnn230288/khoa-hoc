package com.example.demo.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("prod")
@Testcontainers
class ProdProfileIT {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
			.withDatabaseName("prod_profile_it")
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
	void prodDoesNotExposeOpenApiOrSwaggerUi() throws Exception {
		HttpResponse<String> apiDocsResponse = this.client.send(
				HttpRequest.newBuilder(URI.create("http://localhost:" + this.port + "/v3/api-docs"))
						.GET()
						.build(),
				HttpResponse.BodyHandlers.ofString());
		HttpResponse<String> swaggerResponse = this.client.send(
				HttpRequest.newBuilder(URI.create("http://localhost:" + this.port + "/swagger-ui/index.html"))
						.GET()
						.build(),
				HttpResponse.BodyHandlers.ofString());

		assertThat(apiDocsResponse.statusCode()).isEqualTo(404);
		assertThat(swaggerResponse.statusCode()).isEqualTo(404);
	}
}
