package com.chandan.mpgloggerbackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MpgLoggerBackendApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void mainStartsTheApp() {
		// Own database and no web server, so it can't clash with the other tests.
		MpgLoggerBackendApplication.main(new String[] {
				"--spring.main.web-application-type=none",
				"--spring.datasource.url=jdbc:h2:mem:maintest;DB_CLOSE_DELAY=-1"
		});
	}

}
