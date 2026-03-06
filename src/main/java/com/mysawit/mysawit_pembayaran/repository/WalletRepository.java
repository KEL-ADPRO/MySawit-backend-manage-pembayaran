package com.mysawit.mysawit_pembayaran.repository;

import com.mysawit.mysawit_pembayaran.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, String> {
    Optional<Wallet> findByUserId(String userId);
}