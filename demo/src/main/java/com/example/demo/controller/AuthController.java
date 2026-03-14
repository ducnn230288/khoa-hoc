package com.example.demo.controller;

import com.example.demo.dto.CsrfResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.LogoutResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Login, CSRF token, and logout endpoints")
public class AuthController {

    private final AuthenticationManager authenticationManager;

    public AuthController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Operation(
            summary = "Login with username and password",
            description = "Authenticates user and creates a server-side session. Returns Set-Cookie: JSESSIONID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Login successful",
                            content = @Content(schema = @Schema(implementation = LoginResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Invalid credentials (RFC 7807)",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                    .toList();

            return ResponseEntity.ok(new LoginResponse(true, authentication.getName(), roles));

        } catch (AuthenticationException e) {
            ProblemDetail problem = ProblemDetail.forStatus(401);
            problem.setType(URI.create("https://errors.example.com/auth/invalid-credentials"));
            problem.setTitle("Authentication Failed");
            problem.setDetail("Invalid username or password.");
            problem.setInstance(URI.create("/api/v1/auth/login"));
            return ResponseEntity.status(401).body(problem);
        }
    }

    @Operation(
            summary = "Get CSRF token",
            description = "Returns CSRF token for the current session. Requires valid session (JSESSIONID cookie).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "CSRF token returned",
                            content = @Content(schema = @Schema(implementation = CsrfResponse.class))),
                    @ApiResponse(responseCode = "401", description = "No valid session (RFC 7807)",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    @GetMapping("/csrf")
    public ResponseEntity<?> csrf(HttpServletRequest request) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken == null) {
            ProblemDetail problem = ProblemDetail.forStatus(401);
            problem.setType(URI.create("https://errors.example.com/auth/session-required"));
            problem.setTitle("Session Required");
            problem.setDetail("A valid session is required to obtain a CSRF token.");
            problem.setInstance(URI.create("/api/v1/auth/csrf"));
            return ResponseEntity.status(401).body(problem);
        }
        return ResponseEntity.ok(new CsrfResponse(
                csrfToken.getToken(),
                csrfToken.getHeaderName(),
                csrfToken.getParameterName()
        ));
    }

    @Operation(
            summary = "Logout",
            description = "Invalidates session and clears session cookie. Requires valid session + CSRF token.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Logout successful",
                            content = @Content(schema = @Schema(implementation = LogoutResponse.class))),
                    @ApiResponse(responseCode = "401", description = "No valid session (RFC 7807)")
            }
    )
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(new LogoutResponse(true));
    }
}
