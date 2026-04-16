package com.mysawit.pembayaran.repository;

import com.mysawit.pembayaran.model.TopUpTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopUpTransactionRepository extends JpaRepository<TopUpTransaction, UUID> {

    List<TopUpTransaction> findByUserId(UUID userId);

    Optional<TopUpTransaction> findByPaymentGatewayRef(String paymentGatewayRef);
}
