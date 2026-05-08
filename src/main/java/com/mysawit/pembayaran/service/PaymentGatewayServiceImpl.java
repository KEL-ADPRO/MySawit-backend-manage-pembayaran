package com.mysawit.pembayaran.service;

import com.mysawit.pembayaran.client.XenditClient;
import com.mysawit.pembayaran.dto.request.TopUpRequest;
import com.mysawit.pembayaran.dto.response.TopUpResponse;
import com.mysawit.pembayaran.model.TopUpTransaction;
import com.mysawit.pembayaran.model.enums.TopUpStatus;
import com.mysawit.pembayaran.repository.TopUpTransactionRepository;
import com.mysawit.pembayaran.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentGatewayServiceImpl implements PaymentGatewayService {

    private final TopUpTransactionRepository topUpTransactionRepository;
    private final WalletRepository walletRepository;
    private final XenditClient xenditClient;

    @Value("${xendit.exchange-rate:10000}")
    private double exchangeRate;

    @Value("${xendit.amount-step:10000}")
    private double amountStep;

    @Value("${xendit.success-redirect-url:}")
    private String successRedirectUrl;

    @Value("${xendit.failure-redirect-url:}")
    private String failureRedirectUrl;

    @Override
    @Transactional
    public TopUpResponse initiateTopUp(TopUpRequest request) {
        if (request.getAmountRupiah() <= 0 || request.getAmountRupiah() % amountStep != 0) {
            throw new IllegalArgumentException(
                    "amountRupiah must be a positive multiple of " + (long) amountStep
                            + ", got: " + request.getAmountRupiah());
        }

        double amountSawitDollar = request.getAmountRupiah() / exchangeRate;
        String externalId = UUID.randomUUID().toString();
        String description = String.format("TopUp %.0f IDR = %.1f SawitDollar", request.getAmountRupiah(), amountSawitDollar);

        Map<String, Object> xenditResponse = xenditClient.createInvoice(
                externalId, request.getAmountRupiah(), description,
                successRedirectUrl, failureRedirectUrl);

        String paymentGatewayRef = (String) xenditResponse.getOrDefault("external_id", externalId);
        String paymentUrl = (String) xenditResponse.getOrDefault("invoice_url", "");

        TopUpTransaction tx = TopUpTransaction.builder()
                .userId(request.getUserId())
                .amountRupiah(request.getAmountRupiah())
                .amountSawitDollar(amountSawitDollar)
                .paymentGatewayRef(paymentGatewayRef)
                .status(TopUpStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        TopUpTransaction saved = topUpTransactionRepository.save(tx);

        return TopUpResponse.builder()
                .id(saved.getId())
                .userId(saved.getUserId())
                .amountRupiah(saved.getAmountRupiah())
                .amountSawitDollar(saved.getAmountSawitDollar())
                .paymentGatewayRef(saved.getPaymentGatewayRef())
                .paymentUrl(paymentUrl)
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void handleCallback(Map<String, Object> payload) {
        String externalId = (String) payload.get("external_id");
        String status = (String) payload.get("status");

        Optional<TopUpTransaction> txOpt = topUpTransactionRepository.findByPaymentGatewayRef(externalId);
        if (txOpt.isEmpty()) {
            log.warn("No TopUpTransaction found for external_id: {}", externalId);
            return;
        }

        TopUpTransaction tx = txOpt.get();

        if (tx.getStatus() != TopUpStatus.PENDING) {
            log.info("Ignoring duplicate callback for external_id={} — already in terminal state {}",
                    externalId, tx.getStatus());
            return;
        }

        if ("PAID".equals(status)) {
            tx.setStatus(TopUpStatus.SUCCESS);
            topUpTransactionRepository.save(tx);

            walletRepository.findByUserId(tx.getUserId()).ifPresent(wallet -> {
                wallet.setBalance(wallet.getBalance() + tx.getAmountSawitDollar());
                wallet.setUpdatedAt(LocalDateTime.now());
                walletRepository.save(wallet);
            });

        } else if ("EXPIRED".equals(status)) {
            tx.setStatus(TopUpStatus.FAILED);
            topUpTransactionRepository.save(tx);
        }
    }
}
