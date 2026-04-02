package com.example.demo.service;

import java.util.stream.Collectors;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.demo.repository.UserRepository;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

	private final UserRepository userRepository;

	public UserDetailsServiceImpl(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) {
		var user = this.userRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found."));

		return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
				.password(user.getPasswordHash())
				.disabled(!user.isEnabled())
				.authorities(user.getRoles().stream()
						.map(role -> new SimpleGrantedAuthority(role.getCode()))
						.collect(Collectors.toCollection(java.util.LinkedHashSet::new)))
				.build();
	}
}
