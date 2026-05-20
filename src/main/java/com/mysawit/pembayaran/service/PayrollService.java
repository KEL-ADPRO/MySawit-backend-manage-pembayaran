package com.mysawit.pembayaran.service;

import com.mysawit.pembayaran.dto.request.CreatePayrollRequest;
import com.mysawit.pembayaran.dto.request.RejectPayrollRequest;
import com.mysawit.pembayaran.dto.response.PayrollResponse;
import com.mysawit.pembayaran.model.enums.PayrollStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PayrollService {

    PayrollResponse createPayroll(CreatePayrollRequest request);

    List<PayrollResponse> getPayrolls(PayrollStatus status, UUID userId,
                                      LocalDateTime startDate, LocalDateTime endDate);

    PayrollResponse getPayrollById(UUID id);

    PayrollResponse approvePayroll(UUID id, UUID adminUserId);

    PayrollResponse rejectPayroll(UUID id, RejectPayrollRequest request);
}
