package com.mysawit.pembayaran.controller;

import com.mysawit.pembayaran.dto.request.TopUpRequest;
import com.mysawit.pembayaran.dto.response.TopUpResponse;
import com.mysawit.pembayaran.model.enums.TopUpStatus;
import com.mysawit.pembayaran.security.AuthenticatedUser;
import com.mysawit.pembayaran.service.PaymentGatewayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
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
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody TopUpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentGatewayService.initiateTopUp(currentUser.userId(), request));
    }

    @GetMapping
    public ResponseEntity<List<TopUpResponse>> getTopUps(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) TopUpStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(paymentGatewayService.getTopUps(currentUser.userId(), status, startDate, endDate));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TopUpResponse> getTopUp(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID id) {
        return ResponseEntity.ok(paymentGatewayService.getTopUp(currentUser.userId(), id));
    }

    @PostMapping("/{id}/sync")
    public ResponseEntity<TopUpResponse> syncTopUp(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID id) {
        return ResponseEntity.ok(paymentGatewayService.syncTopUp(currentUser.userId(), id));
    }

    @PostMapping("/callback")
    public ResponseEntity<Void> handleCallback(
            @RequestHeader(value = "x-callback-token", required = false) String incomingToken,
            @RequestBody Map<String, Object> payload) {
        if (callbackToken != null && !callbackToken.isBlank()) {
            if (incomingToken == null || !callbackToken.equals(incomingToken)) {
                log.warn("Rejected Xendit callback for external_id={} - token mismatch", payload.get("external_id"));
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }
        paymentGatewayService.handleCallback(payload);
        return ResponseEntity.ok().build();
    }

}
