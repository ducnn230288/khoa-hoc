package com.example.demo.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SessionStoreModeTest {

	@Test
	void doesNotBringSpringSessionOrRedisInfrastructureOntoTheClasspath() {
		assertThatThrownBy(() -> Class.forName("org.springframework.session.SessionRepository"))
				.isInstanceOf(ClassNotFoundException.class);
		assertThatThrownBy(() -> Class.forName("org.springframework.data.redis.connection.RedisConnectionFactory"))
				.isInstanceOf(ClassNotFoundException.class);
	}
}
