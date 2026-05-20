package com.mysawit.pembayaran.repository;

import com.mysawit.pembayaran.model.TopUpTransaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopUpTransactionRepository extends JpaRepository<TopUpTransaction, UUID> {

    List<TopUpTransaction> findByUserId(UUID userId);

    Optional<TopUpTransaction> findByPaymentGatewayRef(String paymentGatewayRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TopUpTransaction> findWithLockingByPaymentGatewayRef(String paymentGatewayRef);
}
