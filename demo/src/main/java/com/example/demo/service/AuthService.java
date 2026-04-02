package com.example.demo.service;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import com.example.demo.audit.AuditLogger;
import com.example.demo.dto.CsrfResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Service
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
	private final SecurityContextRepository securityContextRepository;
	private final AuditLogger auditLogger;
	private final boolean secureSessionCookie;
	private final String sameSiteSessionCookie;

	public AuthService(AuthenticationManager authenticationManager,
			SessionAuthenticationStrategy sessionAuthenticationStrategy,
			SecurityContextRepository securityContextRepository,
			AuditLogger auditLogger,
			@Value("${server.servlet.session.cookie.secure:false}") boolean secureSessionCookie,
			@Value("${server.servlet.session.cookie.same-site:none}") String sameSiteSessionCookie) {
		this.authenticationManager = authenticationManager;
		this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
		this.securityContextRepository = securityContextRepository;
		this.auditLogger = auditLogger;
		this.secureSessionCookie = secureSessionCookie;
		this.sameSiteSessionCookie = normalizeSameSite(sameSiteSessionCookie);
	}

	public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		var authenticationRequest = org.springframework.security.authentication.UsernamePasswordAuthenticationToken
				.unauthenticated(request.username(), request.password());

		try {
			Authentication authentication = this.authenticationManager.authenticate(authenticationRequest);
			this.sessionAuthenticationStrategy.onAuthentication(authentication, httpRequest, httpResponse);

			SecurityContext context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(authentication);
			SecurityContextHolder.setContext(context);
			this.securityContextRepository.saveContext(context, httpRequest, httpResponse);

			this.auditLogger.logLoginSuccess(authentication.getName(), authentication.getAuthorities());

			List<String> roles = authentication.getAuthorities().stream()
					.map(org.springframework.security.core.GrantedAuthority::getAuthority)
					.sorted()
					.toList();

			return new LoginResponse(true, authentication.getName(), roles);
		}
		catch (org.springframework.security.authentication.DisabledException ex) {
			this.auditLogger.logLoginFailure(request.username(), "disabled_user");
			throw ex;
		}
		catch (org.springframework.security.core.AuthenticationException ex) {
			this.auditLogger.logLoginFailure(request.username(), "invalid_credentials");
			throw ex;
		}
	}

	public CsrfResponse csrf(org.springframework.security.web.csrf.CsrfToken csrfToken) {
		return new CsrfResponse(csrfToken.getToken(), csrfToken.getHeaderName(), csrfToken.getParameterName());
	}

	public void logout(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		SecurityContextHolder.clearContext();
		response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from("JSESSIONID", "")
				.httpOnly(true)
				.path("/")
				.maxAge(Duration.ZERO)
				.sameSite(this.sameSiteSessionCookie)
				.secure(this.secureSessionCookie)
				.build()
				.toString());
		this.auditLogger.logLogoutSuccess(authentication.getName());
	}

	private static String normalizeSameSite(String sameSiteSessionCookie) {
		if (sameSiteSessionCookie == null) {
			return "None";
		}

		return switch (sameSiteSessionCookie.trim().toLowerCase(java.util.Locale.ROOT)) {
			case "lax" -> "Lax";
			case "strict" -> "Strict";
			default -> "None";
		};
	}
}
