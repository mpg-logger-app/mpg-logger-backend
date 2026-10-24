package com.chandan.mpgloggerbackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Smoke tests: the whole application starts. */
@SpringBootTest
class MpgLoggerBackendApplicationTests {

	/** The Spring context (all beans, the H2 schema from the entities) starts without errors. */
	@Test
	void contextLoads() {
	}

	/** {@code main} boots the application the same way {@code mvnw spring-boot:run} does. */
	@Test
	void mainStartsTheApp() {
		// Own database and no web server, so it can't clash with the other tests.
		MpgLoggerBackendApplication.main(new String[] {
				"--spring.main.web-application-type=none",
				"--spring.datasource.url=jdbc:h2:mem:maintest;DB_CLOSE_DELAY=-1"
		});
	}

}
