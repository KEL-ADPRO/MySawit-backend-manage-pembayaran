package com.mysawit.mysawit_pembayaran.service;

import com.mysawit.mysawit_pembayaran.dto.response.PayrollResponse;
import java.util.List;

public interface PayrollService {
    List<PayrollResponse> getPayrolls(String userId, String status);
    PayrollResponse getPayrollById(String payrollId);
    PayrollResponse approvePayroll(String payrollId);
    PayrollResponse rejectPayroll(String payrollId, String reason);
}