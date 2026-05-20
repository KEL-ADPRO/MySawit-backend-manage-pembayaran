package com.mysawit.pembayaran.service;

import com.mysawit.pembayaran.dto.request.TopUpRequest;
import com.mysawit.pembayaran.dto.response.TopUpResponse;

import java.util.Map;
import java.util.UUID;

public interface PaymentGatewayService {

    TopUpResponse initiateTopUp(UUID adminUserId, TopUpRequest request);

    void handleCallback(Map<String, Object> payload);
}
