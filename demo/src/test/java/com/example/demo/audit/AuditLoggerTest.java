package com.example.demo.audit;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class AuditLoggerTest {

	private final Logger logger = (Logger) LoggerFactory.getLogger(AuditLogger.class);
	private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
	private final AuditLogger auditLogger = new AuditLogger();

	@BeforeEach
	void setUp() {
		this.appender.start();
		this.logger.addAppender(this.appender);
	}

	@AfterEach
	void tearDown() {
		this.logger.detachAppender(this.appender);
		this.appender.stop();
	}

	@Test
	void writesExpectedAuditEventsWithoutSecrets() {
		this.auditLogger.logLoginSuccess("user01", List.of(new SimpleGrantedAuthority("USER")));
		this.auditLogger.logLoginFailure("user01", "invalid_credentials");
		this.auditLogger.logLogoutSuccess("user01");
		this.auditLogger.logAccessDeniedUnauthenticated("/api/v1/auth/csrf");
		this.auditLogger.logAccessDeniedSessionExpired("/api/v1/auth/csrf");
		this.auditLogger.logAccessDeniedCsrf("/api/v1/auth/logout");

		assertThat(this.appender.list).extracting(ILoggingEvent::getFormattedMessage)
				.anySatisfy(message -> assertThat(message).contains("LOGIN_SUCCESS"))
				.anySatisfy(message -> assertThat(message).contains("LOGIN_FAILURE"))
				.anySatisfy(message -> assertThat(message).contains("LOGOUT_SUCCESS"))
				.noneSatisfy(message -> assertThat(message).contains("User@123"))
				.noneSatisfy(message -> assertThat(message).contains("sessionid-secret"))
				.noneSatisfy(message -> assertThat(message).contains("csrf-secret"));

		assertThat(this.appender.list).extracting(ILoggingEvent::getLevel)
				.contains(Level.INFO, Level.WARN);
	}
}
