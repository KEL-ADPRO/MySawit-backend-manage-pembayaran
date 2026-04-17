package com.mysawit.pembayaran.service;

import com.mysawit.pembayaran.dto.request.TopUpRequest;
import com.mysawit.pembayaran.dto.response.TopUpResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentGatewayServiceImpl implements PaymentGatewayService {

    @Override
    public TopUpResponse initiateTopUp(TopUpRequest request) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void handleCallback(Map<String, Object> payload) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
