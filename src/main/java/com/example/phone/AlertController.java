package com.example.phone;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.google.gson.Gson;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
public class AlertController {

    private static final Logger logger = LoggerFactory.getLogger(AlertController.class);
    private static final String API_DOMAIN = "dyvmsapi.aliyuncs.com";
    private static final String API_VERSION = "2017-05-25";
    private static final String SINGLE_CALL_ACTION = "SingleCallByTts";
    private static final String BATCH_CALL_ACTION = "BatchCallByTts";

    private final Gson gson = new Gson();
    private final IAcsClient acsClient;
    private final Semaphore rateLimiter;

    @Value("${aliyun.voice.template-code}")
    private String templateCode;

    @Value("${aliyun.voice.called-numbers}")
    private String calledNumbers;

    @Value("${alert.webhook.api-key:}")
    private String apiKey;

    public AlertController(IAcsClient acsClient, Semaphore rateLimiter) {
        this.acsClient = acsClient;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/alert")
    public String handleAlert(@RequestBody AlertManagerWebhookRequest request,
                              @RequestHeader(value = "X-API-Key", required = false) String requestApiKey) {
        // 认证校验
        validateApiKey(requestApiKey);

        // 速率限制
        acquireRateLimit();

        try {
            if (!isValidRequest(request)) {
                logger.error("无效的报警请求");
                return "Error: Invalid alert request";
            }

            List<String> validNumbers = preparePhoneNumbers();
            if (validNumbers.isEmpty()) {
                logger.error("无有效号码");
                return "Error: No valid numbers";
            }

            // 处理所有告警，而非仅第一条
            StringBuilder results = new StringBuilder();
            for (AlertManagerWebhookRequest.Alert alert : request.getAlerts()) {
                AlertManagerWebhookRequest.Labels labels = alert.getLabels();
                AlertManagerWebhookRequest.Annotations annotations = alert.getAnnotations();
                String alertname = getAlertValue(labels != null ? labels.getAlertname() : null, "未知报警");
                String severity = getAlertValue(labels != null ? labels.getSeverity() : null, "warning");
                String description = getAlertValue(annotations != null ? annotations.getDescription() : null, "无详细信息");

                logger.info("处理报警: {} [{}] - {}", alertname, severity, description);

                String result = makePhoneCalls(validNumbers, alertname, description);
                results.append(result).append("\n");
            }
            return results.toString().trim();
        } catch (ClientException e) {
            logger.error("阿里云API调用失败", e);
            return "Error: " + e.getMessage();
        } catch (Exception e) {
            logger.error("处理报警失败", e);
            return "Error: " + e.getMessage();
        }
    }

    // ================ 认证与限流 ================ //

    private void validateApiKey(String requestApiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return; // 未配置则跳过校验
        }
        if (!apiKey.equals(requestApiKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API Key");
        }
    }

    private void acquireRateLimit() {
        try {
            if (!rateLimiter.tryAcquire(5, TimeUnit.SECONDS)) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service busy");
        }
    }

    // ================ 核心业务方法 ================ //

    private boolean isValidRequest(AlertManagerWebhookRequest request) {
        return request != null &&
                request.getAlerts() != null &&
                !request.getAlerts().isEmpty();
    }

    private String getAlertValue(String value, String defaultValue) {
        return Objects.toString(value, defaultValue)
                .replace("\"", "")
                .replace("\\", "");
    }

    private List<String> preparePhoneNumbers() {
        return Arrays.stream(calledNumbers.split(","))
                .map(String::trim)
                .filter(this::isValidPhoneNumber)
                .distinct()
                .limit(100)
                .collect(Collectors.toList());
    }

    private boolean isValidPhoneNumber(String number) {
        return number != null && number.matches("^1[3-9]\\d{9}$");
    }

    private String makePhoneCalls(List<String> numbers, String alertname, String description)
            throws ClientException {
        try {
            if (numbers.size() > 1) {
                return batchCall(numbers, alertname, description);
            }
            return singleCall(numbers.get(0), alertname, description);
        } catch (ClientException e) {
            if ("InvalidAction.NotFound".equals(e.getErrCode())) {
                logger.warn("批量呼叫接口不可用，自动降级为单号码轮询");
                return fallbackToSingleCalls(numbers, alertname, description);
            }
            throw e;
        }
    }

    // ================ 阿里云API调用方法 ================ //

    private String singleCall(String number, String alertname, String description)
            throws ClientException {
        CommonRequest request = buildCommonRequest(SINGLE_CALL_ACTION);
        request.putQueryParameter("CalledNumber", number);
        request.putQueryParameter("TtsCode", templateCode);
        request.putQueryParameter("TtsParam", buildTtsParam(alertname, description));

        CommonResponse response = acsClient.getCommonResponse(request);
        logger.info("单号码呼叫结果: {}", response.getData());
        return response.getData();
    }

    private String batchCall(List<String> numbers, String alertname, String description)
            throws ClientException {
        CommonRequest request = buildCommonRequest(BATCH_CALL_ACTION);
        request.putQueryParameter("CalledNumberJson", gson.toJson(numbers));
        request.putQueryParameter("TtsCode", templateCode);
        request.putQueryParameter("TtsParamJson", buildTtsParam(alertname, description));
        request.putQueryParameter("TaskName", "Alert_" + System.currentTimeMillis());

        CommonResponse response = acsClient.getCommonResponse(request);
        logger.info("批量呼叫结果: {}", response.getData());
        return response.getData();
    }

    private String fallbackToSingleCalls(List<String> numbers, String alertname, String description) {
        StringBuilder results = new StringBuilder();
        for (String number : numbers) {
            try {
                String result = singleCall(number, alertname, description);
                results.append(number).append(": 成功 - ").append(result).append("\n");
            } catch (Exception e) {
                results.append(number).append(": 失败 - ").append(e.getMessage()).append("\n");
                logger.error("号码 {} 呼叫失败", number, e);
            }
        }
        return results.toString().trim();
    }

    // ================ 工具方法 ================ //

    private CommonRequest buildCommonRequest(String action) {
        CommonRequest request = new CommonRequest();
        request.setSysMethod(MethodType.POST);
        request.setSysDomain(API_DOMAIN);
        request.setSysVersion(API_VERSION);
        request.setSysAction(action);
        return request;
    }

    private String buildTtsParam(String alertname, String description) {
        return gson.toJson(new TtsParam(alertname, description));
    }

    // ================ 数据结构 ================ //

    @Data
    private static class TtsParam {
        private final String alertname;
        private final String description;
    }

    @Data
    public static class AlertManagerWebhookRequest {
        private List<Alert> alerts;

        @Data
        public static class Alert {
            private Labels labels;
            private Annotations annotations;
        }

        @Data
        public static class Labels {
            private String alertname;
            private String severity;
        }

        @Data
        public static class Annotations {
            private String description;
        }
    }
}
