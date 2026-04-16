package com.mysawit.pembayaran.service;

import java.util.Map;
import java.util.UUID;

public interface PaymentGatewayService {

    String createPaymentLink(UUID topUpTransactionId, double amountRupiah);

    boolean verifyCallback(Map<String, Object> payload);
}
