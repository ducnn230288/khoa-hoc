package com.example.demo;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

	private static final String APP_TIME_ZONE = "UTC";

	public static void main(String[] args) {
		// Keep the JVM and PostgreSQL session on a portable, unambiguous timezone.
		System.setProperty("user.timezone", APP_TIME_ZONE);
		TimeZone.setDefault(TimeZone.getTimeZone(APP_TIME_ZONE));
		SpringApplication.run(DemoApplication.class, args);
	}

}
