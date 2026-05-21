package com.example.phone;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Semaphore;

@Configuration
public class AppConfig {

    @Value("${aliyun.region}")
    private String region;

    @Value("${aliyun.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.access-key-secret}")
    private String accessKeySecret;

    @Value("${alert.rate-limit.permits-per-second:5}")
    private int rateLimitPermits;

    @Bean
    public IAcsClient acsClient() throws ClientException {
        DefaultProfile profile = DefaultProfile.getProfile(region, accessKeyId, accessKeySecret);
        return new DefaultAcsClient(profile);
    }

    @Bean
    public Semaphore rateLimiter() {
        return new Semaphore(rateLimitPermits, true);
    }
}
