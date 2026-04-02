package com.example.demo.audit;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class AuditLogger {

	private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogger.class);

	public void logLoginSuccess(String username, Collection<? extends GrantedAuthority> authorities) {
		LOGGER.info("audit_event=LOGIN_SUCCESS username={} roles={}", sanitize(username), authorities.stream()
				.map(GrantedAuthority::getAuthority)
				.sorted()
				.toList());
	}

	public void logLoginFailure(String username, String reason) {
		LOGGER.warn("audit_event=LOGIN_FAILURE username={} reason={}", sanitize(username), reason);
	}

	public void logLogoutSuccess(String username) {
		LOGGER.info("audit_event=LOGOUT_SUCCESS username={}", sanitize(username));
	}

	public void logAccessDeniedUnauthenticated(String path) {
		LOGGER.warn("audit_event=ACCESS_DENIED_UNAUTHENTICATED path={}", path);
	}

	public void logAccessDeniedSessionExpired(String path) {
		LOGGER.warn("audit_event=ACCESS_DENIED_SESSION_EXPIRED path={}", path);
	}

	public void logAccessDeniedCsrf(String path) {
		LOGGER.warn("audit_event=ACCESS_DENIED_CSRF path={}", path);
	}

	private String sanitize(String value) {
		if (value == null || value.isBlank()) {
			return "anonymous";
		}
		return value;
	}
}
