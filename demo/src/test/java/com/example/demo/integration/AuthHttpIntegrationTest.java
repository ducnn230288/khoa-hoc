package com.example.demo.integration;

import com.example.demo.AbstractIntegrationTest;
import com.example.demo.dto.CsrfResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class AuthHttpIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void csrf_afterLogin_withReturnedSessionCookie_returns200() {
        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<LoginResponse> loginResponse = restTemplate.exchange(
                url("/api/v1/auth/login"),
                HttpMethod.POST,
                new HttpEntity<>(new LoginRequest("user01", "User@123"), loginHeaders),
                LoginResponse.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        String setCookie = loginResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotBlank();
        assertThat(setCookie).contains("JSESSIONID=");
        assertThat(setCookie).contains("HttpOnly");

        HttpHeaders csrfHeaders = new HttpHeaders();
        csrfHeaders.add(HttpHeaders.COOKIE, setCookie.split(";", 2)[0]);

        ResponseEntity<CsrfResponse> csrfResponse = restTemplate.exchange(
                url("/api/v1/auth/csrf"),
                HttpMethod.GET,
                new HttpEntity<>(csrfHeaders),
                CsrfResponse.class);

        assertThat(csrfResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(csrfResponse.getBody()).isNotNull();
        assertThat(csrfResponse.getBody().csrfToken()).isNotBlank();
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
