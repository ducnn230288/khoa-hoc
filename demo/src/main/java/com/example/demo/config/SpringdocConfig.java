package com.example.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"dev", "staging"})
public class SpringdocConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Demo Auth API")
                        .version("1.0.0")
                        .description("Authentication API with session cookie + CSRF. "
                                + "Login to get a JSESSIONID cookie, then call GET /api/v1/auth/csrf "
                                + "to obtain a CSRF token. Include the token in X-CSRF-TOKEN header "
                                + "for all mutating requests (POST/PUT/PATCH/DELETE)."));
    }
}
