package com.mysawit.pembayaran.service;

import com.mysawit.pembayaran.client.XenditClient;
import com.mysawit.pembayaran.dto.request.TopUpRequest;
import com.mysawit.pembayaran.dto.response.TopUpResponse;
import com.mysawit.pembayaran.model.TopUpTransaction;
import com.mysawit.pembayaran.model.enums.TopUpStatus;
import com.mysawit.pembayaran.repository.TopUpTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayServiceImplTest {

    @Mock
    private TopUpTransactionRepository topUpTransactionRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private XenditClient xenditClient;

    @InjectMocks
    private PaymentGatewayServiceImpl paymentGatewayService;

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentGatewayService, "exchangeRate", bd("10000"));
        ReflectionTestUtils.setField(paymentGatewayService, "amountStep", bd("10000"));
        ReflectionTestUtils.setField(paymentGatewayService, "successRedirectUrl", "");
        ReflectionTestUtils.setField(paymentGatewayService, "failureRedirectUrl", "");
    }

    private TopUpRequest buildRequest(UUID suppliedUserId, String amountRupiah) {
        TopUpRequest request = new TopUpRequest();
        request.setUserId(suppliedUserId);
        request.setAmountRupiah(bd(amountRupiah));
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

    private TopUpTransaction buildTransaction(UUID userId, String externalId, TopUpStatus status) {
        return TopUpTransaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .amountRupiah(bd("100000"))
                .amountSawitDollar(bd("10"))
                .paymentGatewayRef(externalId)
                .status(status)
                .createdAt(LocalDateTime.now().minusMinutes(2))
                .build();
    }

    @Test
    void initiateTopUp_validAmount_shouldCreatePendingTransactionForAuthenticatedAdmin() {
        UUID adminId = UUID.randomUUID();
        UUID suppliedUserId = UUID.randomUUID();
        TopUpRequest request = buildRequest(suppliedUserId, "100000");

        when(xenditClient.createInvoice(anyString(), any(BigDecimal.class), anyString(), anyString(), anyString()))
                .thenAnswer(inv -> mockXenditResponse(inv.getArgument(0)));
        when(topUpTransactionRepository.save(any())).thenAnswer(inv -> {
            TopUpTransaction tx = inv.getArgument(0);
            tx.setId(UUID.randomUUID());
            return tx;
        });

        TopUpResponse result = paymentGatewayService.initiateTopUp(adminId, request);

        assertThat(result.getStatus()).isEqualTo(TopUpStatus.PENDING);
        assertThat(result.getUserId()).isEqualTo(adminId);
        assertThat(result.getAmountRupiah()).isEqualByComparingTo("100000.00");
        assertThat(result.getAmountSawitDollar()).isEqualByComparingTo("10.00");
        assertThat(result.getPaymentUrl()).isNotBlank();
        verify(topUpTransactionRepository).save(argThat(tx -> tx.getUserId().equals(adminId)));
        verify(xenditClient).createInvoice(anyString(), eq(bd("100000.00")), anyString(), anyString(), anyString());
    }

    @Test
    void initiateTopUp_amountNotMultipleOf10000_shouldThrow() {
        TopUpRequest request = buildRequest(UUID.randomUUID(), "15000");

        assertThatThrownBy(() -> paymentGatewayService.initiateTopUp(UUID.randomUUID(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10000");
    }

    @Test
    void initiateTopUp_redirectUrlsConfigured_shouldPassToClient() {
        ReflectionTestUtils.setField(paymentGatewayService, "successRedirectUrl", "https://app/success");
        ReflectionTestUtils.setField(paymentGatewayService, "failureRedirectUrl", "https://app/fail");

        TopUpRequest request = buildRequest(UUID.randomUUID(), "100000");

        when(xenditClient.createInvoice(anyString(), any(BigDecimal.class), anyString(), anyString(), anyString()))
                .thenAnswer(inv -> mockXenditResponse(inv.getArgument(0)));
        when(topUpTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentGatewayService.initiateTopUp(UUID.randomUUID(), request);

        verify(xenditClient).createInvoice(
                anyString(), eq(bd("100000.00")), anyString(),
                eq("https://app/success"), eq("https://app/fail"));
    }

    @Test
    void handleCallback_paid_shouldAddBalanceAndSetSuccess() {
        UUID adminId = UUID.randomUUID();
        String externalId = UUID.randomUUID().toString();

        TopUpTransaction tx = TopUpTransaction.builder()
                .id(UUID.randomUUID()).userId(adminId)
                .amountRupiah(bd("100000")).amountSawitDollar(bd("10"))
                .paymentGatewayRef(externalId).status(TopUpStatus.PENDING)
                .createdAt(LocalDateTime.now()).build();

        when(topUpTransactionRepository.findWithLockingByPaymentGatewayRef(externalId)).thenReturn(Optional.of(tx));
        when(topUpTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentGatewayService.handleCallback(Map.of("external_id", externalId, "status", "PAID"));

        verify(topUpTransactionRepository).save(argThat(t -> t.getStatus() == TopUpStatus.SUCCESS));
        verify(walletService).addBalance(adminId, bd("10"));
    }

    @Test
    void handleCallback_expired_shouldSetFailed() {
        UUID adminId = UUID.randomUUID();
        String externalId = UUID.randomUUID().toString();

        TopUpTransaction tx = TopUpTransaction.builder()
                .id(UUID.randomUUID()).userId(adminId)
                .amountRupiah(bd("100000")).amountSawitDollar(bd("10"))
                .paymentGatewayRef(externalId).status(TopUpStatus.PENDING)
                .createdAt(LocalDateTime.now()).build();

        when(topUpTransactionRepository.findWithLockingByPaymentGatewayRef(externalId)).thenReturn(Optional.of(tx));
        when(topUpTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentGatewayService.handleCallback(Map.of("external_id", externalId, "status", "EXPIRED"));

        verify(topUpTransactionRepository).save(argThat(t -> t.getStatus() == TopUpStatus.FAILED));
        verify(walletService, never()).addBalance(any(), any());
    }

    @Test
    void handleCallback_unknownRef_shouldDoNothing() {
        when(topUpTransactionRepository.findWithLockingByPaymentGatewayRef(anyString())).thenReturn(Optional.empty());

        paymentGatewayService.handleCallback(Map.of("external_id", "nonexistent", "status", "PAID"));

        verify(topUpTransactionRepository, never()).save(any());
        verify(walletService, never()).addBalance(any(), any());
    }

    @Test
    void handleCallback_alreadySuccess_shouldNotDoubleCredit() {
        UUID adminId = UUID.randomUUID();
        String externalId = UUID.randomUUID().toString();

        TopUpTransaction tx = TopUpTransaction.builder()
                .id(UUID.randomUUID()).userId(adminId)
                .amountRupiah(bd("100000")).amountSawitDollar(bd("10"))
                .paymentGatewayRef(externalId).status(TopUpStatus.SUCCESS)
                .createdAt(LocalDateTime.now()).build();

        when(topUpTransactionRepository.findWithLockingByPaymentGatewayRef(externalId)).thenReturn(Optional.of(tx));

        paymentGatewayService.handleCallback(Map.of("external_id", externalId, "status", "PAID"));

        verify(topUpTransactionRepository, never()).save(any());
        verify(walletService, never()).addBalance(any(), any());
    }

    @Test
    void getTopUps_shouldReturnUserTransactionsNewestFirst() {
        UUID userId = UUID.randomUUID();
        TopUpTransaction tx = buildTransaction(userId, "ext-ref-1", TopUpStatus.PENDING);
        when(topUpTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(tx));

        List<TopUpResponse> result = paymentGatewayService.getTopUps(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPaymentGatewayRef()).isEqualTo("ext-ref-1");
        verify(topUpTransactionRepository).findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    void getTopUp_forOwner_shouldReturnTransaction() {
        UUID userId = UUID.randomUUID();
        TopUpTransaction tx = buildTransaction(userId, "ext-ref-1", TopUpStatus.PENDING);
        when(topUpTransactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));

        TopUpResponse result = paymentGatewayService.getTopUp(userId, tx.getId());

        assertThat(result.getId()).isEqualTo(tx.getId());
        assertThat(result.getUserId()).isEqualTo(userId);
    }

    @Test
    void getTopUp_forDifferentUser_shouldThrowNotFound() {
        TopUpTransaction tx = buildTransaction(UUID.randomUUID(), "ext-ref-1", TopUpStatus.PENDING);
        when(topUpTransactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> paymentGatewayService.getTopUp(UUID.randomUUID(), tx.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void syncTopUp_paidInvoice_shouldCreditBalanceAndReturnSuccess() {
        UUID userId = UUID.randomUUID();
        TopUpTransaction tx = buildTransaction(userId, "ext-ref-paid", TopUpStatus.PENDING);
        when(topUpTransactionRepository.findWithLockingById(tx.getId())).thenReturn(Optional.of(tx));
        when(xenditClient.getInvoiceByExternalId("ext-ref-paid"))
                .thenReturn(Map.of("external_id", "ext-ref-paid", "status", "PAID"));
        when(topUpTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TopUpResponse result = paymentGatewayService.syncTopUp(userId, tx.getId());

        assertThat(result.getStatus()).isEqualTo(TopUpStatus.SUCCESS);
        verify(topUpTransactionRepository).save(argThat(saved -> saved.getStatus() == TopUpStatus.SUCCESS));
        verify(walletService).addBalance(userId, bd("10"));
    }

    @Test
    void syncTopUp_pendingInvoice_shouldLeaveTransactionPending() {
        UUID userId = UUID.randomUUID();
        TopUpTransaction tx = buildTransaction(userId, "ext-ref-pending", TopUpStatus.PENDING);
        when(topUpTransactionRepository.findWithLockingById(tx.getId())).thenReturn(Optional.of(tx));
        when(xenditClient.getInvoiceByExternalId("ext-ref-pending"))
                .thenReturn(Map.of("external_id", "ext-ref-pending", "status", "PENDING"));

        TopUpResponse result = paymentGatewayService.syncTopUp(userId, tx.getId());

        assertThat(result.getStatus()).isEqualTo(TopUpStatus.PENDING);
        verify(topUpTransactionRepository, never()).save(any());
        verify(walletService, never()).addBalance(any(), any());
    }

    @Test
    void reconcilePendingTopUps_shouldPollOldPendingTransactions() {
        UUID userId = UUID.randomUUID();
        TopUpTransaction paid = buildTransaction(userId, "ext-ref-paid", TopUpStatus.PENDING);
        TopUpTransaction pending = buildTransaction(userId, "ext-ref-pending", TopUpStatus.PENDING);
        when(topUpTransactionRepository.findByStatusAndCreatedAtBefore(eq(TopUpStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(paid, pending));
        when(topUpTransactionRepository.findWithLockingById(paid.getId())).thenReturn(Optional.of(paid));
        when(topUpTransactionRepository.findWithLockingById(pending.getId())).thenReturn(Optional.of(pending));
        when(xenditClient.getInvoiceByExternalId("ext-ref-paid")).thenReturn(Map.of("status", "PAID"));
        when(xenditClient.getInvoiceByExternalId("ext-ref-pending")).thenReturn(Map.of("status", "PENDING"));
        when(topUpTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int synced = paymentGatewayService.reconcilePendingTopUps();

        assertThat(synced).isEqualTo(1);
        verify(walletService).addBalance(userId, bd("10"));
        verify(topUpTransactionRepository).save(argThat(saved -> saved.getStatus() == TopUpStatus.SUCCESS));
    }
}
