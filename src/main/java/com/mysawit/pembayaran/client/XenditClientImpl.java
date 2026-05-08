package com.mysawit.pembayaran.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class XenditClientImpl implements XenditClient {

    private static final String XENDIT_INVOICE_URL = "https://api.xendit.co/v2/invoices";

    @Value("${xendit.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public XenditClientImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public Map<String, Object> createInvoice(String externalId,
                                             double amountRupiah,
                                             String description,
                                             String successRedirectUrl,
                                             String failureRedirectUrl) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("XENDIT_API_KEY not set — returning mock invoice for {}", externalId);
            Map<String, Object> mock = new HashMap<>();
            mock.put("id", externalId);
            mock.put("external_id", externalId);
            mock.put("invoice_url", "https://mock-payment.xendit.co/pay/" + externalId);
            mock.put("status", "PENDING");
            return mock;
        }

        String basicAuth = "Basic " + Base64.getEncoder().encodeToString((apiKey + ":").getBytes());

        Map<String, Object> body = new HashMap<>();
        body.put("external_id", externalId);
        body.put("amount", amountRupiah);
        body.put("description", description);
        if (successRedirectUrl != null && !successRedirectUrl.isBlank()) {
            body.put("success_redirect_url", successRedirectUrl);
        }
        if (failureRedirectUrl != null && !failureRedirectUrl.isBlank()) {
            body.put("failure_redirect_url", failureRedirectUrl);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", basicAuth);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                XENDIT_INVOICE_URL, HttpMethod.POST, entity,
                new org.springframework.core.ParameterizedTypeReference<>() {});
        return response.getBody();
    }
}
