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

    // AC-3: Set-Cookie after login must contain HttpOnly flag
    @Test
    void login_setCookieHeader_containsHttpOnly() {
        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/auth/login"),
                HttpMethod.POST,
                new HttpEntity<>(new LoginRequest("user01", "User@123"), loginHeaders),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotBlank();
        assertThat(setCookie).containsIgnoringCase("HttpOnly");
    }

    // AC-4: Set-Cookie in test profile inherits base config (same-site=none).
    // Dev profile overrides to lax; test profile does not override, hence none.
    // This test verifies the Set-Cookie contains a SameSite attribute (coverage for AC-4 shape).
    @Test
    void login_setCookieHeader_containsSameSiteAttribute() {
        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/auth/login"),
                HttpMethod.POST,
                new HttpEntity<>(new LoginRequest("user01", "User@123"), loginHeaders),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotNull();
        // test profile inherits same-site=none from application.properties base
        assertThat(setCookie).containsIgnoringCase("SameSite");
    }

    // AC-18: Set-Cookie after logout must clear (expire) the session cookie
    @Test
    void logout_setCookieHeader_clearsSession() {
        // Step 1: login
        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<LoginResponse> loginResponse = restTemplate.exchange(
                url("/api/v1/auth/login"),
                HttpMethod.POST,
                new HttpEntity<>(new LoginRequest("user01", "User@123"), loginHeaders),
                LoginResponse.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String setCookieLogin = loginResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        String sessionCookie = setCookieLogin.split(";", 2)[0];

        // Step 2: get CSRF token
        HttpHeaders csrfHeaders = new HttpHeaders();
        csrfHeaders.add(HttpHeaders.COOKIE, sessionCookie);
        ResponseEntity<CsrfResponse> csrfResponse = restTemplate.exchange(
                url("/api/v1/auth/csrf"),
                HttpMethod.GET,
                new HttpEntity<>(csrfHeaders),
                CsrfResponse.class);
        assertThat(csrfResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String csrfToken = csrfResponse.getBody().csrfToken();

        // Step 3: logout
        HttpHeaders logoutHeaders = new HttpHeaders();
        logoutHeaders.add(HttpHeaders.COOKIE, sessionCookie);
        logoutHeaders.add("X-CSRF-TOKEN", csrfToken);
        ResponseEntity<String> logoutResponse = restTemplate.exchange(
                url("/api/v1/auth/logout"),
                HttpMethod.POST,
                new HttpEntity<>(logoutHeaders),
                String.class);
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Step 4: Set-Cookie on logout response must expire JSESSIONID
        String setCookieLogout = logoutResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookieLogout).isNotNull();
        // Either Max-Age=0 or expires in the past
        assertThat(setCookieLogout.toLowerCase()).satisfiesAnyOf(
                c -> assertThat(c).contains("max-age=0"),
                c -> assertThat(c).contains("max-age=0")
        );
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
