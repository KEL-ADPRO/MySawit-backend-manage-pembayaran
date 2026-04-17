package com.mysawit.pembayaran.service;

import com.mysawit.pembayaran.dto.request.CreatePayrollRequest;
import com.mysawit.pembayaran.dto.request.RejectPayrollRequest;
import com.mysawit.pembayaran.dto.response.PayrollResponse;
import com.mysawit.pembayaran.model.enums.PayrollStatus;
import com.mysawit.pembayaran.repository.PayrollRepository;
import com.mysawit.pembayaran.repository.WageConfigRepository;
import com.mysawit.pembayaran.service.strategy.WageCalculatorFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    public static final UUID ADMIN_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final PayrollRepository payrollRepository;
    private final WageConfigRepository wageConfigRepository;
    private final WageCalculatorFactory wageCalculatorFactory;
    private final WalletService walletService;

    @Override
    public PayrollResponse createPayroll(CreatePayrollRequest request) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public List<PayrollResponse> getPayrolls(PayrollStatus status, UUID userId,
                                              LocalDateTime startDate, LocalDateTime endDate) {
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
