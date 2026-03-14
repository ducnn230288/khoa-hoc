package com.example.demo.controller;

import com.example.demo.dto.CsrfResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.LogoutResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Auth endpoints: login, csrf token, logout.
 * AR-B2: Controller handles HTTP only — no business logic.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Session-based authentication endpoints")
public class AuthController {

    private final AuthenticationManager authenticationManager;

    public AuthController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    /**
     * AC-1, AC-2, AC-6, AC-7
     */
    @Operation(summary = "Login with username and password",
               description = "Authenticates the user and creates a server-side session. " +
                             "Returns session cookie via Set-Cookie header.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful — Set-Cookie: JSESSIONID"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials or account disabled (RFC 7807)")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        SecurityContext sc = SecurityContextHolder.createEmptyContext();
        sc.setAuthentication(auth);
        SecurityContextHolder.setContext(sc);
        // Persist security context in the HTTP session
        new HttpSessionSecurityContextRepository().saveContext(sc, httpRequest, httpResponse);

        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .toList();

        return ResponseEntity.ok(new LoginResponse(true, auth.getName(), roles));
    }

    /**
     * AC-9, AC-10, AC-11
     */
    @Operation(summary = "Get CSRF token",
               description = "Returns the CSRF token bound to the current session. " +
                             "Requires a valid session cookie. Frontend must call this after login and on app init.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "CSRF token returned"),
        @ApiResponse(responseCode = "401", description = "No valid session (RFC 7807)")
    })
    @GetMapping("/csrf")
    public ResponseEntity<CsrfResponse> csrf(@RequestAttribute(required = false) CsrfToken csrfToken) {
        // If session doesn't exist the SecurityFilterChain returns 401 before reaching here
        if (csrfToken == null) {
            throw new org.springframework.security.authentication.InsufficientAuthenticationException(
                    "No CSRF token available for this session.");
        }
        return ResponseEntity.ok(new CsrfResponse(
                csrfToken.getToken(),
                csrfToken.getHeaderName(),
                csrfToken.getParameterName()));
    }

    /**
     * AC-16, AC-17, AC-18
     */
    @Operation(summary = "Logout",
               description = "Invalidates the current session and clears the session cookie. " +
                             "Requires valid session + X-CSRF-TOKEN header.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logged out successfully"),
        @ApiResponse(responseCode = "401", description = "No valid session"),
        @ApiResponse(responseCode = "403", description = "Missing or invalid CSRF token (RFC 7807)")
    })
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate(); // AC-17: invalidate session server-side
        }
        SecurityContextHolder.clearContext();
        // AC-18: clear session cookie
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("JSESSIONID", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        return ResponseEntity.ok(new LogoutResponse(true));
    }
}
