package com.example.phone;

import com.aliyuncs.CommonResponse;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.example.phone.AlertController.AlertManagerWebhookRequest;
import com.example.phone.AlertController.AlertManagerWebhookRequest.Alert;
import com.example.phone.AlertController.AlertManagerWebhookRequest.Annotations;
import com.example.phone.AlertController.AlertManagerWebhookRequest.Labels;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertController.class)
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Gson gson;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public IAcsClient acsClient() throws ClientException {
            IAcsClient client = mock(IAcsClient.class);
            CommonResponse response = new CommonResponse();
            response.setData("{\"CallId\":\"test-call-id\"}");
            when(client.getCommonResponse(any())).thenReturn(response);
            return client;
        }

        @Bean
        public Semaphore rateLimiter() {
            return new Semaphore(5, true);
        }
    }

    @Test
    void shouldProcessValidSingleAlert() throws Exception {
        AlertManagerWebhookRequest request = buildRequest("HighCPU", "critical", "CPU usage > 90%");

        mockMvc.perform(post("/alert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gson.toJson(request)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldProcessMultipleAlerts() throws Exception {
        Alert alert1 = buildAlert("HighCPU", "critical", "CPU usage > 90%");
        Alert alert2 = buildAlert("DiskFull", "warning", "Disk usage > 80%");
        AlertManagerWebhookRequest request = new AlertManagerWebhookRequest();
        request.setAlerts(List.of(alert1, alert2));

        mockMvc.perform(post("/alert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gson.toJson(request)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnErrorForEmptyAlerts() throws Exception {
        AlertManagerWebhookRequest request = new AlertManagerWebhookRequest();
        request.setAlerts(Collections.emptyList());

        mockMvc.perform(post("/alert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gson.toJson(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Error: Invalid alert request"));
    }

    @Test
    void shouldReturnErrorForNullAlerts() throws Exception {
        AlertManagerWebhookRequest request = new AlertManagerWebhookRequest();
        request.setAlerts(null);

        mockMvc.perform(post("/alert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alerts\":null}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Error: Invalid alert request"));
    }

    @Test
    void shouldHandleMissingLabels() throws Exception {
        String json = "{\"alerts\":[{\"labels\":null,\"annotations\":null}]}";

        mockMvc.perform(post("/alert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }

    private AlertManagerWebhookRequest buildRequest(String name, String severity, String description) {
        AlertManagerWebhookRequest request = new AlertManagerWebhookRequest();
        request.setAlerts(List.of(buildAlert(name, severity, description)));
        return request;
    }

    private Alert buildAlert(String name, String severity, String description) {
        Labels labels = new Labels();
        labels.setAlertname(name);
        labels.setSeverity(severity);

        Annotations annotations = new Annotations();
        annotations.setDescription(description);

        Alert alert = new Alert();
        alert.setLabels(labels);
        alert.setAnnotations(annotations);
        return alert;
    }
}
