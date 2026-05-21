package com.mysawit.pembayaran.service;

import com.mysawit.pembayaran.dto.request.CreatePayrollRequest;
import com.mysawit.pembayaran.dto.request.DriverDeliveryPayrollEventRequest;
import com.mysawit.pembayaran.dto.request.FactoryDeliveryPayrollEventRequest;
import com.mysawit.pembayaran.dto.request.HarvestPayrollEventRequest;
import com.mysawit.pembayaran.dto.response.PayrollResponse;
import com.mysawit.pembayaran.model.enums.PayrollSourceType;
import com.mysawit.pembayaran.model.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class PayrollIntegrationServiceImpl implements PayrollIntegrationService {

    private final PayrollService payrollService;

    @Override
    @Async
    public CompletableFuture<PayrollResponse> createFromHarvestApproval(HarvestPayrollEventRequest request) {
        CreatePayrollRequest payrollRequest = new CreatePayrollRequest();
        payrollRequest.setUserId(request.getBuruhUserId());
        payrollRequest.setUserRole(UserRole.BURUH);
        payrollRequest.setSourceType(PayrollSourceType.HARVEST_APPROVAL);
        payrollRequest.setSourceId(request.getSourceId());
        payrollRequest.setIdempotencyKey(request.getIdempotencyKey());
        payrollRequest.setHarvestedKg(request.getHarvestedKg());
        return CompletableFuture.completedFuture(payrollService.createPayroll(payrollRequest));
    }

    @Override
    @Async
    public CompletableFuture<PayrollResponse> createFromDriverDeliveryApproval(DriverDeliveryPayrollEventRequest request) {
        CreatePayrollRequest payrollRequest = new CreatePayrollRequest();
        payrollRequest.setUserId(request.getSupirUserId());
        payrollRequest.setUserRole(UserRole.SUPIR_TRUK);
        payrollRequest.setSourceType(PayrollSourceType.DRIVER_DELIVERY_APPROVAL);
        payrollRequest.setSourceId(request.getSourceId());
        payrollRequest.setIdempotencyKey(request.getIdempotencyKey());
        payrollRequest.setDeliveredKg(request.getDeliveredKg());
        return CompletableFuture.completedFuture(payrollService.createPayroll(payrollRequest));
    }

    @Override
    @Async
    public CompletableFuture<PayrollResponse> createFromFactoryDeliveryApproval(FactoryDeliveryPayrollEventRequest request) {
        CreatePayrollRequest payrollRequest = new CreatePayrollRequest();
        payrollRequest.setUserId(request.getMandorUserId());
        payrollRequest.setUserRole(UserRole.MANDOR);
        payrollRequest.setSourceType(PayrollSourceType.FACTORY_DELIVERY_APPROVAL);
        payrollRequest.setSourceId(request.getSourceId());
        payrollRequest.setIdempotencyKey(request.getIdempotencyKey());
        payrollRequest.setRecognizedKg(request.getRecognizedKg());
        return CompletableFuture.completedFuture(payrollService.createPayroll(payrollRequest));
    }
}
