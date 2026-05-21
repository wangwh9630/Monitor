package com.example.phone;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"aliyun.access-key-id=test-key-id",
		"aliyun.access-key-secret=test-key-secret",
		"alert.webhook.api-key="
})
class PhoneApplicationTests {

	@Test
	void contextLoads() {
	}

}
