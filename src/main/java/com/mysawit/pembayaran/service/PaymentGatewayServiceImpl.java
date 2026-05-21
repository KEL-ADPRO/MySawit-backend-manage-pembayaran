package com.mysawit.pembayaran.service;

import com.mysawit.pembayaran.client.XenditClient;
import com.mysawit.pembayaran.dto.request.TopUpRequest;
import com.mysawit.pembayaran.dto.response.TopUpResponse;
import com.mysawit.pembayaran.model.TopUpTransaction;
import com.mysawit.pembayaran.model.enums.TopUpStatus;
import com.mysawit.pembayaran.repository.TopUpTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentGatewayServiceImpl implements PaymentGatewayService {

    private final TopUpTransactionRepository topUpTransactionRepository;
    private final WalletService walletService;
    private final XenditClient xenditClient;

    @Value("${xendit.exchange-rate:10000}")
    private BigDecimal exchangeRate;

    @Value("${xendit.amount-step:10000}")
    private BigDecimal amountStep;

    @Value("${xendit.success-redirect-url:}")
    private String successRedirectUrl;

    @Value("${xendit.failure-redirect-url:}")
    private String failureRedirectUrl;

    @Override
    @Transactional
    public TopUpResponse initiateTopUp(UUID adminUserId, TopUpRequest request) {
        BigDecimal amountRupiah = normalizeMoney(request.getAmountRupiah());
        if (amountRupiah.signum() <= 0 || amountRupiah.remainder(amountStep).compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException(
                    "amountRupiah must be a positive multiple of " + format(amountStep)
                            + ", got: " + format(amountRupiah));
        }

        BigDecimal amountSawitDollar = normalizeMoney(amountRupiah.divide(exchangeRate, 2, RoundingMode.HALF_UP));
        String externalId = UUID.randomUUID().toString();
        String description = "TopUp " + format(amountRupiah) + " IDR = " + format(amountSawitDollar) + " SawitDollar";

        Map<String, Object> xenditResponse = xenditClient.createInvoice(
                externalId, amountRupiah, description,
                successRedirectUrl, failureRedirectUrl);

        String paymentGatewayRef = (String) xenditResponse.getOrDefault("external_id", externalId);
        String paymentUrl = (String) xenditResponse.getOrDefault("invoice_url", "");

        TopUpTransaction tx = TopUpTransaction.builder()
                .userId(adminUserId)
                .amountRupiah(amountRupiah)
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

        Optional<TopUpTransaction> txOpt = topUpTransactionRepository.findWithLockingByPaymentGatewayRef(externalId);
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
            walletService.addBalance(tx.getUserId(), tx.getAmountSawitDollar());

        } else if ("EXPIRED".equals(status)) {
            tx.setStatus(TopUpStatus.FAILED);
            topUpTransactionRepository.save(tx);
        }
    }

    private BigDecimal normalizeMoney(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("amountRupiah is required");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String format(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }
}
