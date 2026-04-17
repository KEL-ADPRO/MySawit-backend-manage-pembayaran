package com.mysawit.pembayaran.service;

import com.mysawit.pembayaran.dto.request.TopUpRequest;
import com.mysawit.pembayaran.dto.response.TopUpResponse;

import java.util.Map;

public interface PaymentGatewayService {

    TopUpResponse initiateTopUp(TopUpRequest request);

    void handleCallback(Map<String, Object> payload);
}
