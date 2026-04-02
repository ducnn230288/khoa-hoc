package com.example.demo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringdocConfig {

	@Bean
	OpenAPI authOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Demo Authentication API")
						.version("v1")
						.description("Session cookie authentication with CSRF rehydration via GET /api/v1/auth/csrf."))
				.components(new Components()
						.addSecuritySchemes("sessionCookie", new SecurityScheme()
								.type(SecurityScheme.Type.APIKEY)
								.in(SecurityScheme.In.COOKIE)
								.name("JSESSIONID")
								.description("Server-side session cookie"))
						.addSecuritySchemes("csrfHeader", new SecurityScheme()
								.type(SecurityScheme.Type.APIKEY)
								.in(SecurityScheme.In.HEADER)
								.name("X-CSRF-TOKEN")
								.description("CSRF token issued by GET /api/v1/auth/csrf")));
	}
}
