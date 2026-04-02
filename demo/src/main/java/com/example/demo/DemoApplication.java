package com.example.demo;

import java.util.Map;
import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

	private static final Map<String, String> TIMEZONE_ALIASES = Map.of(
			"Asia/Saigon", "Asia/Ho_Chi_Minh");

	static {
		normalizeJvmTimezoneAlias();
	}

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	static String normalizeTimezoneId(String timezoneId) {
		return TIMEZONE_ALIASES.getOrDefault(timezoneId, timezoneId);
	}

	static void normalizeJvmTimezoneAlias() {
		String currentTimezoneId = TimeZone.getDefault().getID();
		String normalizedTimezoneId = normalizeTimezoneId(currentTimezoneId);
		if (!currentTimezoneId.equals(normalizedTimezoneId)) {
			TimeZone.setDefault(TimeZone.getTimeZone(normalizedTimezoneId));
			System.setProperty("user.timezone", normalizedTimezoneId);
		}
	}

}
