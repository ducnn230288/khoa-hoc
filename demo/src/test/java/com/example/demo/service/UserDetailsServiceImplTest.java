package com.example.demo.service;

import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TST-3, TST-5: Unit tests with Mockito — naming: method_state_expectedBehaviour.
 */
@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserDetailsServiceImpl service;

    @Test
    void loadUserByUsername_withValidUser_returnsUserDetails() {
        Role role = mockRole("USER");
        User user = mockUser("user01", "$2b$12$hash", true, Set.of(role));
        when(userRepository.findByUsername("user01")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("user01");

        assertThat(details.getUsername()).isEqualTo("user01");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    void loadUserByUsername_withDisabledUser_returnsDisabledUserDetails() {
        Role role = mockRole("USER");
        User user = mockUser("user01", "$2b$12$hash", false, Set.of(role));
        when(userRepository.findByUsername("user01")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("user01");

        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    void loadUserByUsername_withUnknownUser_throwsUsernameNotFoundException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    // --- helpers ---

    private static Role mockRole(String code) {
        Role r = mock(Role.class);
        when(r.getCode()).thenReturn(code);
        return r;
    }

    private static User mockUser(String username, String pwHash, boolean enabled, Set<Role> roles) {
        User u = mock(User.class);
        when(u.getUsername()).thenReturn(username);
        when(u.getPasswordHash()).thenReturn(pwHash);
        when(u.isEnabled()).thenReturn(enabled);
        when(u.getRoles()).thenReturn(roles);
        return u;
    }
}
