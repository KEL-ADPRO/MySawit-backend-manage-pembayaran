package com.mysawit.mysawit_pembayaran.service.impl;

import com.mysawit.mysawit_pembayaran.dto.request.UpdatePayrollRateRequest;
import com.mysawit.mysawit_pembayaran.dto.response.PayrollRateResponse;
import com.mysawit.mysawit_pembayaran.model.PayrollRateConfig;
import com.mysawit.mysawit_pembayaran.repository.PayrollRateConfigRepository;
import com.mysawit.mysawit_pembayaran.service.PayrollRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PayrollRateServiceImpl implements PayrollRateService {

    private final PayrollRateConfigRepository payrollRateConfigRepository;

    @Override
    public PayrollRateResponse getPayrollRate() {
        PayrollRateConfig config = payrollRateConfigRepository.findAll()
                .stream()
                .findFirst()
                .orElseGet(() -> payrollRateConfigRepository.save(new PayrollRateConfig()));

        return toResponse(config);
    }

    @Override
    public PayrollRateResponse updatePayrollRate(UpdatePayrollRateRequest request) {
        PayrollRateConfig config = payrollRateConfigRepository.findAll()
                .stream()
                .findFirst()
                .orElseGet(PayrollRateConfig::new);

        config.setWorkerRatePerKg(request.getWorkerRatePerKg());
        config.setDriverRatePerKg(request.getDriverRatePerKg());
        config.setForemanRatePerKg(request.getForemanRatePerKg());

        return toResponse(payrollRateConfigRepository.save(config));
    }

    private PayrollRateResponse toResponse(PayrollRateConfig config) {
        return PayrollRateResponse.builder()
                .id(config.getId())
                .workerRatePerKg(config.getWorkerRatePerKg())
                .driverRatePerKg(config.getDriverRatePerKg())
                .foremanRatePerKg(config.getForemanRatePerKg())
                .build();
    }
}