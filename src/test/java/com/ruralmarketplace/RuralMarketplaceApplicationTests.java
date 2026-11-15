package com.ruralmarketplace;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:velnora-context;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
class RuralMarketplaceApplicationTests {

	@DynamicPropertySource
	static void configureTestJwtSecret(DynamicPropertyRegistry registry) {
		TestJwtProperties.register(registry);
	}

	@Test
	void contextLoads() {
	}

}
