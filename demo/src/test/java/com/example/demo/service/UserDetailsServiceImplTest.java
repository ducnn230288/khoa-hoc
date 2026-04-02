package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

	@Mock
	private UserRepository userRepository;

	@Test
	void loadsEnabledUserWithAuthorities() {
		User user = new User();
		user.setUsername("user01");
		user.setPasswordHash("encoded-password");
		user.setEnabled(true);
		user.setCreatedAt(Instant.now());
		user.setUpdatedAt(Instant.now());

		Role role = new Role();
		role.setCode("USER");
		role.setName("User");
		user.setRoles(Set.of(role));

		when(this.userRepository.findByUsername("user01")).thenReturn(Optional.of(user));

		UserDetailsServiceImpl service = new UserDetailsServiceImpl(this.userRepository);
		var userDetails = service.loadUserByUsername("user01");

		assertThat(userDetails.getUsername()).isEqualTo("user01");
		assertThat(userDetails.getPassword()).isEqualTo("encoded-password");
		assertThat(userDetails.isEnabled()).isTrue();
		assertThat(userDetails.getAuthorities()).extracting("authority").containsExactly("USER");
	}

	@Test
	void marksDisabledUsersAsDisabled() {
		User user = new User();
		user.setUsername("disabled-user");
		user.setPasswordHash("encoded-password");
		user.setEnabled(false);
		user.setCreatedAt(Instant.now());
		user.setUpdatedAt(Instant.now());

		when(this.userRepository.findByUsername("disabled-user")).thenReturn(Optional.of(user));

		UserDetailsServiceImpl service = new UserDetailsServiceImpl(this.userRepository);
		var userDetails = service.loadUserByUsername("disabled-user");

		assertThat(userDetails.isEnabled()).isFalse();
	}

	@Test
	void throwsWhenUserIsMissing() {
		when(this.userRepository.findByUsername("missing")).thenReturn(Optional.empty());

		UserDetailsServiceImpl service = new UserDetailsServiceImpl(this.userRepository);

		assertThatThrownBy(() -> service.loadUserByUsername("missing"))
				.isInstanceOf(UsernameNotFoundException.class)
				.hasMessage("User not found.");
	}
}
