package com.mysawit.pembayaran.service;

import com.mysawit.pembayaran.dto.request.CreatePayrollRequest;
import com.mysawit.pembayaran.dto.request.RejectPayrollRequest;
import com.mysawit.pembayaran.dto.response.PayrollResponse;
import com.mysawit.pembayaran.model.enums.PayrollStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    @Override
    public PayrollResponse createPayroll(CreatePayrollRequest request) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public List<PayrollResponse> getPayrolls(PayrollStatus status, UUID userId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public PayrollResponse getPayrollById(UUID id) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public PayrollResponse approvePayroll(UUID id) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public PayrollResponse rejectPayroll(UUID id, RejectPayrollRequest request) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
