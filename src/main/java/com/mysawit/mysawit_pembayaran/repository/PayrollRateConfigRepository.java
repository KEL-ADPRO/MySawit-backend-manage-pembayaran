package com.mysawit.mysawit_pembayaran.repository;

import com.mysawit.mysawit_pembayaran.model.PayrollRateConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollRateConfigRepository extends JpaRepository<PayrollRateConfig, String> {
}