package com.mysawit.pembayaran.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class TopUpReconcileJobTest {

    @Test
    void reconcilePendingTopUps_shouldDelegateToPaymentGatewayService() {
        PaymentGatewayService paymentGatewayService = mock(PaymentGatewayService.class);
        when(paymentGatewayService.reconcilePendingTopUps()).thenReturn(2);
        TopUpReconcileJob job = new TopUpReconcileJob(paymentGatewayService);

        job.reconcilePendingTopUps();

        verify(paymentGatewayService).reconcilePendingTopUps();
    }
}
