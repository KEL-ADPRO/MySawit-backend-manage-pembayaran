package com.mysawit.pembayaran.controller;

import com.mysawit.pembayaran.dto.request.TopUpRequest;
import com.mysawit.pembayaran.dto.response.TopUpResponse;
import com.mysawit.pembayaran.security.AuthenticatedUser;
import com.mysawit.pembayaran.service.PaymentGatewayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/pembayaran/wallet/topup")
@RequiredArgsConstructor
public class PaymentGatewayController {

    private final PaymentGatewayService paymentGatewayService;

    @Value("${xendit.callback-token:}")
    private String callbackToken;

    @PostMapping
    public ResponseEntity<TopUpResponse> initiateTopUp(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody TopUpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentGatewayService.initiateTopUp(currentUser.userId(), request));
    }

    @PostMapping("/callback")
    public ResponseEntity<Void> handleCallback(
            @RequestHeader(value = "x-callback-token", required = false) String incomingToken,
            @RequestBody Map<String, Object> payload) {
        if (callbackToken != null && !callbackToken.isBlank()) {
            if (incomingToken == null || !callbackToken.equals(incomingToken)) {
                log.warn("Rejected Xendit callback for external_id={} — token mismatch", payload.get("external_id"));
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }
        paymentGatewayService.handleCallback(payload);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/mock-pay/{externalId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> showMockPaymentPage(@PathVariable String externalId) {
        return ResponseEntity.ok(renderMockPaymentPage(externalId));
    }

    @PostMapping(value = "/mock-pay/{externalId}/paid", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> markMockPaymentPaid(@PathVariable String externalId) {
        paymentGatewayService.handleCallback(Map.of("external_id", externalId, "status", "PAID"));
        return ResponseEntity.ok(renderMockPaymentResultPage(externalId, "SUCCESS"));
    }

    @PostMapping(value = "/mock-pay/{externalId}/failed", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> markMockPaymentFailed(@PathVariable String externalId) {
        paymentGatewayService.handleCallback(Map.of("external_id", externalId, "status", "EXPIRED"));
        return ResponseEntity.ok(renderMockPaymentResultPage(externalId, "FAILED"));
    }

    private String renderMockPaymentPage(String externalId) {
        String safeExternalId = HtmlUtils.htmlEscape(externalId);
        String encodedExternalId = UriUtils.encodePathSegment(externalId, StandardCharsets.UTF_8);
        return """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>Mock Xendit Payment</title>
                    <style>
                        body { font-family: system-ui, sans-serif; margin: 40px; color: #172033; }
                        main { max-width: 520px; }
                        code { background: #eef1f6; padding: 3px 6px; border-radius: 4px; }
                        button { border: 0; border-radius: 6px; padding: 10px 14px; cursor: pointer; }
                        .paid { background: #0f7b45; color: white; }
                        .failed { background: #b3261e; color: white; margin-left: 8px; }
                    </style>
                </head>
                <body>
                    <main>
                        <h1>Mock Xendit Payment</h1>
                        <p>External ID: <code>%s</code></p>
                        <form method="post" action="/api/pembayaran/wallet/topup/mock-pay/%s/paid" style="display:inline">
                            <button class="paid" type="submit">Mark as paid</button>
                        </form>
                        <form method="post" action="/api/pembayaran/wallet/topup/mock-pay/%s/failed" style="display:inline">
                            <button class="failed" type="submit">Mark as failed</button>
                        </form>
                    </main>
                </body>
                </html>
                """.formatted(safeExternalId, encodedExternalId, encodedExternalId);
    }

    private String renderMockPaymentResultPage(String externalId, String status) {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>Mock Payment %s</title>
                    <style>
                        body { font-family: system-ui, sans-serif; margin: 40px; color: #172033; }
                        code { background: #eef1f6; padding: 3px 6px; border-radius: 4px; }
                    </style>
                </head>
                <body>
                    <main>
                        <h1>Payment %s</h1>
                        <p>External ID: <code>%s</code></p>
                        <p>You can close this tab and refresh the frontend.</p>
                    </main>
                </body>
                </html>
                """.formatted(status, status, HtmlUtils.htmlEscape(externalId));
    }

}
