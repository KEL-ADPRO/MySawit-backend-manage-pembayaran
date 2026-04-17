package com.mysawit.pembayaran.controller;

import com.mysawit.pembayaran.dto.request.TopUpRequest;
import com.mysawit.pembayaran.dto.response.TopUpResponse;
import com.mysawit.pembayaran.service.PaymentGatewayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pembayaran/wallet/topup")
@RequiredArgsConstructor
public class PaymentGatewayController {

    private final PaymentGatewayService paymentGatewayService;

    @PostMapping
    public ResponseEntity<TopUpResponse> initiateTopUp(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @Valid @RequestBody TopUpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentGatewayService.initiateTopUp(request));
    }

    @PostMapping("/callback")
    public ResponseEntity<Void> handleCallback(@RequestBody Map<String, Object> payload) {
        paymentGatewayService.handleCallback(payload);
        return ResponseEntity.ok().build();
    }

    private boolean isAdmin(String userRole) {
        return "ADMIN".equals(userRole);
    }
}
