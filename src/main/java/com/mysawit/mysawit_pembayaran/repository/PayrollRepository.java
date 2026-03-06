package com.mysawit.mysawit_pembayaran.repository;

import com.mysawit.mysawit_pembayaran.model.Payroll;
import com.mysawit.mysawit_pembayaran.model.PayrollStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PayrollRepository extends JpaRepository<Payroll, String> {
    List<Payroll> findByRecipientUserId(String recipientUserId);
    List<Payroll> findByRecipientUserIdAndStatus(String recipientUserId, PayrollStatus status);
}