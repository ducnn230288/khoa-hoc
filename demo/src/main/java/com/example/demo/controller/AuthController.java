package com.example.demo.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.dto.CsrfResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Login with username and password",
			description = "Creates a server-side session cookie after successful authentication.")
	@ApiResponse(responseCode = "200", description = "Authenticated",
			content = @Content(schema = @Schema(implementation = LoginResponse.class)))
	@ApiResponse(responseCode = "401", description = "Invalid credentials or disabled user")
	public LoginResponse login(@RequestBody LoginRequest request, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		return this.authService.login(request, httpRequest, httpResponse);
	}

	@GetMapping("/csrf")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Get CSRF token",
			description = "Returns the CSRF token for the current session and rehydrates auth state after reload.",
			security = @SecurityRequirement(name = "sessionCookie"))
	@ApiResponse(responseCode = "200", description = "CSRF token issued",
			content = @Content(schema = @Schema(implementation = CsrfResponse.class),
					examples = @ExampleObject(value = """
							{"csrfToken":"generated-token-value","headerName":"X-CSRF-TOKEN","parameterName":"_csrf"}
							""")))
	@ApiResponse(responseCode = "401", description = "Session required or expired")
	public CsrfResponse csrf(HttpServletRequest request) {
		Object attribute = request.getAttribute(CsrfToken.class.getName());
		if (!(attribute instanceof CsrfToken csrfToken)) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "CSRF token is unavailable.");
		}
		return this.authService.csrf(csrfToken);
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Logout current session",
			description = "Requires a valid session cookie and X-CSRF-TOKEN header.",
			security = { @SecurityRequirement(name = "sessionCookie"), @SecurityRequirement(name = "csrfHeader") })
	@ApiResponse(responseCode = "200", description = "Logged out")
	@ApiResponse(responseCode = "401", description = "Session required or expired")
	@ApiResponse(responseCode = "403", description = "CSRF token missing or invalid")
	public Map<String, Boolean> logout(Authentication authentication, HttpServletRequest request,
			HttpServletResponse response) {
		this.authService.logout(authentication, request, response);
		return Map.of("success", true);
	}
}
