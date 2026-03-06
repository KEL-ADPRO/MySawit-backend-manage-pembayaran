package com.mysawit.mysawit_pembayaran.repository;

import com.mysawit.mysawit_pembayaran.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, String> {
    List<PaymentTransaction> findByWalletId(String walletId);
}