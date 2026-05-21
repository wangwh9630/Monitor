package com.example.phone;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"aliyun.access-key-id=test-key-id",
		"aliyun.access-key-secret=test-key-secret",
		"aliyun.region=cn-hangzhou",
		"aliyun.voice.template-code=TTS_TEST",
		"alert.webhook.api-key=",
		"alert.rate-limit.permits-per-second=5",
		"spring.datasource.url=jdbc:h2:mem:testdb",
		"spring.datasource.password=",
		"spring.h2.console.enabled=false"
})
class PhoneApplicationTests {

	@Test
	void contextLoads() {
	}

}
