package com.mysawit.pembayaran.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "topup.reconcile.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class TopUpReconcileJob {

    private final PaymentGatewayService paymentGatewayService;

    @Scheduled(fixedDelay = 60_000)
    public void reconcilePendingTopUps() {
        int synced = paymentGatewayService.reconcilePendingTopUps();
        if (synced > 0) {
            log.info("Reconciled {} pending top-up transaction(s)", synced);
        }
    }
}
