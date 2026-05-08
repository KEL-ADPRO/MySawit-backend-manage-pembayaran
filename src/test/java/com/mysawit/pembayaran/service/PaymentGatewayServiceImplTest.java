package com.mysawit.pembayaran.service;

import com.mysawit.pembayaran.client.XenditClient;
import com.mysawit.pembayaran.dto.request.TopUpRequest;
import com.mysawit.pembayaran.dto.response.TopUpResponse;
import com.mysawit.pembayaran.model.TopUpTransaction;
import com.mysawit.pembayaran.model.Wallet;
import com.mysawit.pembayaran.model.enums.TopUpStatus;
import com.mysawit.pembayaran.repository.TopUpTransactionRepository;
import com.mysawit.pembayaran.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayServiceImplTest {

    @Mock
    private TopUpTransactionRepository topUpTransactionRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private XenditClient xenditClient;

    @InjectMocks
    private PaymentGatewayServiceImpl paymentGatewayService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentGatewayService, "exchangeRate", 10000.0);
        ReflectionTestUtils.setField(paymentGatewayService, "amountStep", 10000.0);
        ReflectionTestUtils.setField(paymentGatewayService, "successRedirectUrl", "");
        ReflectionTestUtils.setField(paymentGatewayService, "failureRedirectUrl", "");
    }

    private TopUpRequest buildRequest(UUID userId, double amountRupiah) {
        TopUpRequest request = new TopUpRequest();
        request.setUserId(userId);
        request.setAmountRupiah(amountRupiah);
        return request;
    }

    private Map<String, Object> mockXenditResponse(String externalId) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", "xendit-" + externalId);
        response.put("external_id", externalId);
        response.put("invoice_url", "https://mock-payment.xendit.co/pay/" + externalId);
        response.put("status", "PENDING");
        return response;
    }

    // ─── initiateTopUp ───────────────────────────────────────────────────────

    @Test
    void initiateTopUp_validAmount_shouldCreatePendingTransaction() {
        UUID userId = UUID.randomUUID();
        TopUpRequest request = buildRequest(userId, 100000.0);

        when(xenditClient.createInvoice(anyString(), anyDouble(), anyString(), anyString(), anyString()))
                .thenAnswer(inv -> mockXenditResponse(inv.getArgument(0)));
        when(topUpTransactionRepository.save(any())).thenAnswer(inv -> {
            TopUpTransaction tx = inv.getArgument(0);
            tx = TopUpTransaction.builder()
                    .id(UUID.randomUUID())
                    .userId(tx.getUserId())
                    .amountRupiah(tx.getAmountRupiah())
                    .amountSawitDollar(tx.getAmountSawitDollar())
                    .paymentGatewayRef(tx.getPaymentGatewayRef())
                    .status(tx.getStatus())
                    .createdAt(tx.getCreatedAt())
                    .build();
            return tx;
        });

        TopUpResponse result = paymentGatewayService.initiateTopUp(request);

        assertThat(result.getStatus()).isEqualTo(TopUpStatus.PENDING);
        assertThat(result.getAmountRupiah()).isEqualTo(100000.0);
        assertThat(result.getAmountSawitDollar()).isEqualTo(10.0);
        assertThat(result.getPaymentUrl()).isNotBlank();
        verify(xenditClient).createInvoice(anyString(), eq(100000.0), anyString(), anyString(), anyString());
    }

    @Test
    void initiateTopUp_amountNotMultipleOf10000_shouldThrow() {
        UUID userId = UUID.randomUUID();
        TopUpRequest request = buildRequest(userId, 15000.0);

        assertThatThrownBy(() -> paymentGatewayService.initiateTopUp(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10000");
    }

    @Test
    void initiateTopUp_correctExchangeRate_shouldConvert() {
        UUID userId = UUID.randomUUID();
        TopUpRequest request = buildRequest(userId, 50000.0);

        when(xenditClient.createInvoice(anyString(), anyDouble(), anyString(), anyString(), anyString()))
                .thenAnswer(inv -> mockXenditResponse(inv.getArgument(0)));
        when(topUpTransactionRepository.save(any())).thenAnswer(inv -> {
            TopUpTransaction tx = inv.getArgument(0);
            return TopUpTransaction.builder()
                    .id(UUID.randomUUID()).userId(tx.getUserId())
                    .amountRupiah(tx.getAmountRupiah()).amountSawitDollar(tx.getAmountSawitDollar())
                    .paymentGatewayRef(tx.getPaymentGatewayRef()).status(tx.getStatus())
                    .createdAt(tx.getCreatedAt()).build();
        });

        TopUpResponse result = paymentGatewayService.initiateTopUp(request);

        assertThat(result.getAmountSawitDollar()).isEqualTo(5.0);
    }

    @Test
    void initiateTopUp_redirectUrlsConfigured_shouldPassToClient() {
        ReflectionTestUtils.setField(paymentGatewayService, "successRedirectUrl", "https://app/success");
        ReflectionTestUtils.setField(paymentGatewayService, "failureRedirectUrl", "https://app/fail");

        UUID userId = UUID.randomUUID();
        TopUpRequest request = buildRequest(userId, 100000.0);

        when(xenditClient.createInvoice(anyString(), anyDouble(), anyString(), anyString(), anyString()))
                .thenAnswer(inv -> mockXenditResponse(inv.getArgument(0)));
        when(topUpTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentGatewayService.initiateTopUp(request);

        verify(xenditClient).createInvoice(
                anyString(), eq(100000.0), anyString(),
                eq("https://app/success"), eq("https://app/fail"));
    }

    // ─── handleCallback ──────────────────────────────────────────────────────

    @Test
    void handleCallback_paid_shouldAddBalanceAndSetSuccess() {
        UUID userId = UUID.randomUUID();
        String externalId = UUID.randomUUID().toString();

        TopUpTransaction tx = TopUpTransaction.builder()
                .id(UUID.randomUUID()).userId(userId)
                .amountRupiah(100000.0).amountSawitDollar(10.0)
                .paymentGatewayRef(externalId).status(TopUpStatus.PENDING)
                .createdAt(LocalDateTime.now()).build();

        Wallet wallet = Wallet.builder()
                .id(UUID.randomUUID()).userId(userId).balance(50.0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        when(topUpTransactionRepository.findByPaymentGatewayRef(externalId)).thenReturn(Optional.of(tx));
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(topUpTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> payload = new HashMap<>();
        payload.put("external_id", externalId);
        payload.put("status", "PAID");

        paymentGatewayService.handleCallback(payload);

        verify(walletRepository).save(argThat(w -> w.getBalance() == 60.0));
        verify(topUpTransactionRepository).save(argThat(t -> t.getStatus() == TopUpStatus.SUCCESS));
    }

    @Test
    void handleCallback_expired_shouldSetFailed() {
        UUID userId = UUID.randomUUID();
        String externalId = UUID.randomUUID().toString();

        TopUpTransaction tx = TopUpTransaction.builder()
                .id(UUID.randomUUID()).userId(userId)
                .amountRupiah(100000.0).amountSawitDollar(10.0)
                .paymentGatewayRef(externalId).status(TopUpStatus.PENDING)
                .createdAt(LocalDateTime.now()).build();

        when(topUpTransactionRepository.findByPaymentGatewayRef(externalId)).thenReturn(Optional.of(tx));
        when(topUpTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> payload = new HashMap<>();
        payload.put("external_id", externalId);
        payload.put("status", "EXPIRED");

        paymentGatewayService.handleCallback(payload);

        verify(topUpTransactionRepository).save(argThat(t -> t.getStatus() == TopUpStatus.FAILED));
        verify(walletRepository, never()).findByUserId(any());
    }

    @Test
    void handleCallback_unknownRef_shouldDoNothing() {
        when(topUpTransactionRepository.findByPaymentGatewayRef(anyString())).thenReturn(Optional.empty());

        Map<String, Object> payload = new HashMap<>();
        payload.put("external_id", "nonexistent");
        payload.put("status", "PAID");

        paymentGatewayService.handleCallback(payload);

        verify(topUpTransactionRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void handleCallback_alreadySuccess_shouldNotDoubleCredit() {
        UUID userId = UUID.randomUUID();
        String externalId = UUID.randomUUID().toString();

        TopUpTransaction tx = TopUpTransaction.builder()
                .id(UUID.randomUUID()).userId(userId)
                .amountRupiah(100000.0).amountSawitDollar(10.0)
                .paymentGatewayRef(externalId).status(TopUpStatus.SUCCESS)
                .createdAt(LocalDateTime.now()).build();

        when(topUpTransactionRepository.findByPaymentGatewayRef(externalId)).thenReturn(Optional.of(tx));

        Map<String, Object> payload = new HashMap<>();
        payload.put("external_id", externalId);
        payload.put("status", "PAID");

        paymentGatewayService.handleCallback(payload);

        verify(topUpTransactionRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
        verify(walletRepository, never()).findByUserId(any());
    }
}
