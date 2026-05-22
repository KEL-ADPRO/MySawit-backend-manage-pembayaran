package com.mysawit.pembayaran.repository;

import com.mysawit.pembayaran.model.TopUpTransaction;
import com.mysawit.pembayaran.model.enums.TopUpStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopUpTransactionRepository extends JpaRepository<TopUpTransaction, UUID> {

    List<TopUpTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<TopUpTransaction> findByStatusAndCreatedAtBefore(TopUpStatus status, LocalDateTime createdAt);

    Optional<TopUpTransaction> findByPaymentGatewayRef(String paymentGatewayRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TopUpTransaction> findWithLockingByPaymentGatewayRef(String paymentGatewayRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TopUpTransaction> findWithLockingById(UUID id);
}
