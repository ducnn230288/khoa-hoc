package com.example.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Springdoc OpenAPI configuration.
 * SEC-8, AC-21: This bean is only active in dev and staging profiles.
 * In prod, springdoc.api-docs.enabled=false and springdoc.swagger-ui.enabled=false
 * prevent all endpoints from being exposed.
 */
@Configuration
@Profile({"dev", "staging"})
public class SpringdocConfig {

    @Bean
    public OpenAPI authOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Demo Auth API")
                        .description("Session-based authentication with CSRF protection.")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList("sessionCookie"))
                .schemaRequirement("sessionCookie",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("JSESSIONID")
                                .description("Session cookie — set automatically by login endpoint"));
    }
}
