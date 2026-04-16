package com.mysawit.pembayaran.repository;

import com.mysawit.pembayaran.model.Payroll;
import com.mysawit.pembayaran.model.enums.PayrollStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, UUID> {

    List<Payroll> findByUserId(UUID userId);

    List<Payroll> findByStatus(PayrollStatus status);

    List<Payroll> findByUserIdAndStatus(UUID userId, PayrollStatus status);
}
