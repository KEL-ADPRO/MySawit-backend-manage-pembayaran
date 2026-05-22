package com.mysawit.pembayaran.service;

import com.mysawit.pembayaran.dto.request.TopUpRequest;
import com.mysawit.pembayaran.dto.response.TopUpResponse;
import com.mysawit.pembayaran.model.enums.TopUpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PaymentGatewayService {

    TopUpResponse initiateTopUp(UUID adminUserId, TopUpRequest request);

    List<TopUpResponse> getTopUps(UUID userId, TopUpStatus status, LocalDateTime startDate, LocalDateTime endDate);

    TopUpResponse getTopUp(UUID userId, UUID topUpId);

    TopUpResponse syncTopUp(UUID userId, UUID topUpId);

    int reconcilePendingTopUps();

    void handleCallback(Map<String, Object> payload);
}
