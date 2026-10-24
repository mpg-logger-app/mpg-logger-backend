package com.chandan.mpgloggerbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the MPG Logger REST API (Spring Boot).
 * Fuel logs live in {@code fuellog}; accounts, sessions and sign-in live in {@code user}.
 */
@SpringBootApplication
public class MpgLoggerBackendApplication {

	/**
	 * Starts the application.
	 *
	 * @param args command-line arguments, passed on to Spring Boot (e.g. {@code --server.port=8081})
	 */
	public static void main(String[] args) {
		SpringApplication.run(MpgLoggerBackendApplication.class, args);
	}

}
