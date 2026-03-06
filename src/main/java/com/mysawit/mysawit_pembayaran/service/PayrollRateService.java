package com.mysawit.mysawit_pembayaran.service;

import com.mysawit.mysawit_pembayaran.dto.request.UpdatePayrollRateRequest;
import com.mysawit.mysawit_pembayaran.dto.response.PayrollRateResponse;

public interface PayrollRateService {
    PayrollRateResponse getPayrollRate();
    PayrollRateResponse updatePayrollRate(UpdatePayrollRateRequest request);
}