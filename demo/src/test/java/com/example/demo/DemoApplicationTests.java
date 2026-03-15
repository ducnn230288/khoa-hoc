package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Context load smoke test using Testcontainers PostgreSQL.
 * TST-2: Real PostgreSQL, not mocked.
 * Extends AbstractIntegrationTest to get @DynamicPropertySource for Testcontainers.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class DemoApplicationTests extends AbstractIntegrationTest {

	@Test
	void contextLoads() {
	}

}
