package com.mysawit.pembayaran.controller;

import com.mysawit.pembayaran.dto.request.TopUpRequest;
import com.mysawit.pembayaran.dto.response.TopUpResponse;
import com.mysawit.pembayaran.service.PaymentGatewayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

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
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Id", required = false) UUID requesterId,
            @Valid @RequestBody TopUpRequest request) {
        if (!RequestAuthorization.isAdmin(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (requesterId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (request.getUserId() != null && !requesterId.equals(request.getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        request.setUserId(requesterId);
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentGatewayService.initiateTopUp(request));
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
}
