package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;

import com.example.demo.audit.AuditLogger;
import com.example.demo.dto.LoginRequest;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private SessionAuthenticationStrategy sessionAuthenticationStrategy;

	@Mock
	private SecurityContextRepository securityContextRepository;

	@Mock
	private AuditLogger auditLogger;

	@AfterEach
	void clearSecurityContext() {
		org.springframework.security.core.context.SecurityContextHolder.clearContext();
	}

	@Test
	void loginCreatesAnAuthenticatedResponseAndPersistsTheSecurityContext() {
		Authentication authentication = mock(Authentication.class);
		java.util.Collection<? extends GrantedAuthority> authorities = List.of(
				new SimpleGrantedAuthority("USER"),
				new SimpleGrantedAuthority("ADMIN"));
		when(authentication.getName()).thenReturn("user01");
		doReturn(authorities).when(authentication).getAuthorities();
		when(this.authenticationManager.authenticate(any())).thenReturn(authentication);

		AuthService service = new AuthService(
				this.authenticationManager,
				this.sessionAuthenticationStrategy,
				this.securityContextRepository,
				this.auditLogger,
				false,
				"none");
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		var result = service.login(new LoginRequest("user01", "User@123"), request, response);

		assertThat(result.authenticated()).isTrue();
		assertThat(result.username()).isEqualTo("user01");
		assertThat(result.roles()).containsExactly("ADMIN", "USER");
		assertThat(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication())
				.isSameAs(authentication);
		verify(this.sessionAuthenticationStrategy).onAuthentication(authentication, request, response);
		verify(this.securityContextRepository).saveContext(any(), any(), any());
		verify(this.auditLogger).logLoginSuccess("user01", authentication.getAuthorities());
	}

	@Test
	void loginLogsDisabledUsersWithTheDedicatedReasonAndRethrows() {
		when(this.authenticationManager.authenticate(any())).thenThrow(new DisabledException("Disabled"));

		AuthService service = new AuthService(
				this.authenticationManager,
				this.sessionAuthenticationStrategy,
				this.securityContextRepository,
				this.auditLogger,
				false,
				"none");

		assertThatThrownBy(() -> service.login(
				new LoginRequest("disabled-user", "Disabled@123"),
				new MockHttpServletRequest(),
				new MockHttpServletResponse()))
				.isInstanceOf(DisabledException.class);

		verify(this.auditLogger).logLoginFailure("disabled-user", "disabled_user");
		verifyNoInteractions(this.sessionAuthenticationStrategy, this.securityContextRepository);
	}

	@Test
	void loginLogsInvalidCredentialsAndRethrows() {
		when(this.authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

		AuthService service = new AuthService(
				this.authenticationManager,
				this.sessionAuthenticationStrategy,
				this.securityContextRepository,
				this.auditLogger,
				false,
				"none");

		assertThatThrownBy(() -> service.login(
				new LoginRequest("user01", "wrong-password"),
				new MockHttpServletRequest(),
				new MockHttpServletResponse()))
				.isInstanceOf(BadCredentialsException.class);

		verify(this.auditLogger).logLoginFailure("user01", "invalid_credentials");
		verifyNoInteractions(this.sessionAuthenticationStrategy, this.securityContextRepository);
	}

	@Test
	void csrfCopiesTheTokenContractIntoTheResponsePayload() {
		CsrfToken csrfToken = mock(CsrfToken.class);
		when(csrfToken.getToken()).thenReturn("csrf-1");
		when(csrfToken.getHeaderName()).thenReturn("X-CSRF-TOKEN");
		when(csrfToken.getParameterName()).thenReturn("_csrf");

		AuthService service = new AuthService(
				this.authenticationManager,
				this.sessionAuthenticationStrategy,
				this.securityContextRepository,
				this.auditLogger,
				false,
				null);

		var response = service.csrf(csrfToken);

		assertThat(response.csrfToken()).isEqualTo("csrf-1");
		assertThat(response.headerName()).isEqualTo("X-CSRF-TOKEN");
		assertThat(response.parameterName()).isEqualTo("_csrf");
	}

	@Test
	void logoutInvalidatesTheCurrentSessionAndClearsTheCookie() {
		Authentication authentication = mock(Authentication.class);
		when(authentication.getName()).thenReturn("user01");

		AuthService service = new AuthService(
				this.authenticationManager,
				this.sessionAuthenticationStrategy,
				this.securityContextRepository,
				this.auditLogger,
				true,
				"strict");
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpSession session = (MockHttpSession) request.getSession(true);
		MockHttpServletResponse response = new MockHttpServletResponse();

		service.logout(authentication, request, response);

		assertThat(session.isInvalid()).isTrue();
		assertThat(response.getHeaders("Set-Cookie")).singleElement()
				.satisfies(cookie -> assertThat(cookie)
						.contains("JSESSIONID=")
						.contains("HttpOnly")
						.contains("Path=/")
						.contains("Max-Age=0")
						.contains("Secure")
						.contains("SameSite=Strict"));
		verify(this.auditLogger).logLogoutSuccess("user01");
	}
}
