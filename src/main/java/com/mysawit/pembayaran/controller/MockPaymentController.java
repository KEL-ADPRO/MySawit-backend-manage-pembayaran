package com.mysawit.pembayaran.controller;

import com.mysawit.pembayaran.service.PaymentGatewayService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@Profile("dev")
@RequestMapping("/api/pembayaran/wallet/topup/mock-pay")
@RequiredArgsConstructor
public class MockPaymentController {

    private final PaymentGatewayService paymentGatewayService;

    @GetMapping(value = "/{externalId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> showMockPaymentPage(@PathVariable String externalId) {
        return ResponseEntity.ok(renderMockPaymentPage(externalId));
    }

    @PostMapping(value = "/{externalId}/paid", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> markMockPaymentPaid(@PathVariable String externalId) {
        paymentGatewayService.handleCallback(Map.of("external_id", externalId, "status", "PAID"));
        return ResponseEntity.ok(renderMockPaymentResultPage(externalId, "SUCCESS"));
    }

    @PostMapping(value = "/{externalId}/failed", produces = MediaType.TEXT_HTML_VALUE)
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
