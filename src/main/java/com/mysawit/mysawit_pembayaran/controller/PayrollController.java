package com.mysawit.mysawit_pembayaran.controller;

import com.mysawit.mysawit_pembayaran.dto.request.RejectPayrollRequest;
import com.mysawit.mysawit_pembayaran.dto.response.PayrollResponse;
import com.mysawit.mysawit_pembayaran.service.PayrollService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payrolls")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    @GetMapping
    public List<PayrollResponse> getPayrolls(
            @RequestParam String userId,
            @RequestParam(required = false) String status
    ) {
        return payrollService.getPayrolls(userId, status);
    }

    @GetMapping("/{id}")
    public PayrollResponse getPayrollById(@PathVariable String id) {
        return payrollService.getPayrollById(id);
    }

    @PatchMapping("/{id}/approve")
    public PayrollResponse approvePayroll(@PathVariable String id) {
        return payrollService.approvePayroll(id);
    }

    @PatchMapping("/{id}/reject")
    public PayrollResponse rejectPayroll(
            @PathVariable String id,
            @Valid @RequestBody RejectPayrollRequest request
    ) {
        return payrollService.rejectPayroll(id, request.getReason());
    }
}