package com.example.demo.config;

import java.util.List;

import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import jakarta.annotation.PostConstruct;

@Configuration
public class FlywayLocationGuard {

	private final Environment environment;
	private final FlywayProperties flywayProperties;

	public FlywayLocationGuard(Environment environment, FlywayProperties flywayProperties) {
		this.environment = environment;
		this.flywayProperties = flywayProperties;
	}

	@PostConstruct
	void validateLocations() {
		List<String> locations = this.flywayProperties.getLocations();
		boolean hasSeedLocation = locations.stream().anyMatch(location -> location.contains("db/seed"));
		if (hasSeedLocation && this.environment.acceptsProfiles(Profiles.of("staging", "prod"))) {
			throw new IllegalStateException("Non-prod Flyway seed location is not allowed for staging/prod profiles.");
		}
	}
}
