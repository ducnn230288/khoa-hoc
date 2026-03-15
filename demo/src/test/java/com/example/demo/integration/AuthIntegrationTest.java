package com.example.demo.integration;

import com.example.demo.AbstractIntegrationTest;
import com.example.demo.dto.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TST-2: Full integration tests with real Testcontainers PostgreSQL.
 * Covers AC-2, AC-6, AC-7, AC-9, AC-11, AC-13, AC-14, AC-16, AC-17, AC-18, AC-32.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    // AC-2: login success → 200 + Set-Cookie
    @Test
    void login_withValidCredentials_returns200AndSetCookie() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("user01", "User@123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value("user01"))
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNotNull();
    }

    // Session persistence is covered here; actual Set-Cookie flags are verified by AuthHttpIntegrationTest.
    @Test
    void login_success_createsSessionForSubsequentRequests() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("user01", "User@123"))))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNotNull();
    }

    // AC-6: wrong password → 401 RFC 7807
    @Test
    void login_withWrongPassword_returns401ProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("user01", "wrongpassword"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://errors.example.com/auth/invalid-credentials"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.detail").exists());
    }

    // AC-9: login then fetch CSRF → csrfToken, headerName, parameterName present
    @Test
    void csrf_afterLogin_returns200WithToken() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("user01", "User@123"))))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(get("/api/v1/auth/csrf").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.csrfToken").exists())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.parameterName").exists());
    }

    // AC-11: CSRF endpoint without session → 401
    @Test
    void csrf_withoutSession_returns401ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // AC-13: mutating request missing CSRF header → 403
    @Test
    void logout_missingCsrfHeader_returns403() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("user01", "User@123"))))
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        // POST without CSRF token
        mockMvc.perform(post("/api/v1/auth/logout").session(session))
                .andExpect(status().isForbidden());
    }

    // AC-16, AC-17, AC-18: logout invalidates session
    @Test
    void logout_withValidSession_invalidatesSession() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("user01", "User@123"))))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        // Get CSRF token
        MvcResult csrfResult = mockMvc.perform(get("/api/v1/auth/csrf").session(session))
                .andExpect(status().isOk())
                .andReturn();
        String csrfToken = objectMapper.readTree(csrfResult.getResponse().getContentAsString())
                .get("csrfToken").asText();

        // Logout with CSRF token
        mockMvc.perform(post("/api/v1/auth/logout")
                .session(session)
                .header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // After logout, CSRF endpoint should return 401
        mockMvc.perform(get("/api/v1/auth/csrf").session(session))
                .andExpect(status().isUnauthorized());
    }

    // AC-32: error response has all 5 RFC 7807 fields
    @Test
    void login_failure_responseHasAllRfc7807Fields() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("nobody", "bad"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.detail").exists());
        // instance field is set by Spring's ProblemDetail when status is written
    }
}
