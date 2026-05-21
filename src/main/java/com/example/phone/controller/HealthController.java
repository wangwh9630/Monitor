package com.example.phone.controller;

import com.example.phone.service.PhoneNumberService;
import com.example.phone.service.TtsTemplateService;
import com.aliyuncs.IAcsClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final IAcsClient acsClient;
    private final PhoneNumberService phoneNumberService;
    private final TtsTemplateService ttsTemplateService;

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("status", "UP");
        info.put("timestamp", LocalDateTime.now().toString());
        info.put("acsClient", acsClient != null ? "connected" : "disconnected");
        info.put("phoneNumbers", phoneNumberService.findEnabledNumbers().size());
        info.put("activeTemplate", ttsTemplateService.getActiveTemplateCode());
        return info;
    }
}
