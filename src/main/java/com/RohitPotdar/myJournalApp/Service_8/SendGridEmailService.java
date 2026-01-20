package com.RohitPotdar.myJournalApp.Service_8;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SendGridEmailService {
    private static final String SENDGRID_ENDPOINT = "https://api.sendgrid.com/v3/mail/send";

    @Value("${sendgrid.api.key:}")
    private String sendGridApiKey;

    @Value("${sendgrid.from.email:}")
    private String fromEmail;

    @Value("${sendgrid.from.name:Quendora}")
    private String fromName;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public SendGridEmailService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public void sendResetCode(String toEmail, String code, int ttlMinutes) {
        if (sendGridApiKey == null || sendGridApiKey.trim().isEmpty()) {
            throw new IllegalStateException("SendGrid API key not configured.");
        }
        if (fromEmail == null || fromEmail.trim().isEmpty()) {
            throw new IllegalStateException("SendGrid sender email not configured.");
        }
        if (toEmail == null || toEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient email is required.");
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("from", Map.of("email", fromEmail.trim(), "name", fromName));
            payload.put("personalizations", List.of(
                    Map.of("to", List.of(Map.of("email", toEmail.trim())))
            ));
            payload.put("subject", "Quendora Password Reset Code");
            String contentText = "Your password reset code is: " + code + "\n\n" +
                    "This code will expire in " + ttlMinutes + " minutes.\n" +
                    "If you did not request this, please ignore this email.";
            payload.put("content", List.of(
                    Map.of("type", "text/plain", "value", contentText)
            ));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(sendGridApiKey.trim());

            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    SENDGRID_ENDPOINT,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("SendGrid error: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send reset code: " + e.getMessage(), e);
        }
    }
}
