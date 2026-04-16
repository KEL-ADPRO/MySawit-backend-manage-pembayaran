package com.mysawit.pembayaran.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentGatewayServiceImpl implements PaymentGatewayService {

    @Override
    public String createPaymentLink(UUID topUpTransactionId, double amountRupiah) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public boolean verifyCallback(Map<String, Object> payload) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
