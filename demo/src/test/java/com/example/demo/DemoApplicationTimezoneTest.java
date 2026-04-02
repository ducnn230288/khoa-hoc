package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DemoApplicationTimezoneTest {

	@Test
	void normalizesAsiaSaigonToCanonicalVietnamTimezone() {
		assertThat(DemoApplication.normalizeTimezoneId("Asia/Saigon"))
				.isEqualTo("Asia/Ho_Chi_Minh");
	}

	@Test
	void leavesAlreadySupportedTimezoneIdsUntouched() {
		assertThat(DemoApplication.normalizeTimezoneId("UTC")).isEqualTo("UTC");
	}
}
