package com.example.phone.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final RestTemplate restTemplate;

    @GetMapping
    public String page() {
        return "test";
    }

    @PostMapping
    public String sendTest(@RequestParam String alertname,
                           @RequestParam(defaultValue = "warning") String severity,
                           @RequestParam(defaultValue = "测试告警") String description,
                           RedirectAttributes ra) {
        Map<String, Object> request = Map.of(
                "alerts", List.of(Map.of(
                        "labels", Map.of("alertname", alertname, "severity", severity),
                        "annotations", Map.of("description", description)
                ))
        );
        try {
            String result = restTemplate.postForObject("http://localhost:8001/alert", request, String.class);
            ra.addFlashAttribute("result", result);
        } catch (RestClientException e) {
            ra.addFlashAttribute("result", "Error: " + e.getMessage());
        }
        ra.addFlashAttribute("alertname", alertname);
        return "redirect:/test";
    }
}
