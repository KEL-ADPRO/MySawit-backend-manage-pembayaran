package com.mysawit.pembayaran.controller;

import com.mysawit.pembayaran.dto.request.CreatePayrollRequest;
import com.mysawit.pembayaran.dto.request.RejectPayrollRequest;
import com.mysawit.pembayaran.dto.response.PayrollResponse;
import com.mysawit.pembayaran.model.enums.PayrollStatus;
import com.mysawit.pembayaran.service.PayrollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pembayaran/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    @PostMapping
    public ResponseEntity<PayrollResponse> createPayroll(
            @RequestHeader(value = "X-User-Id", required = false) UUID requesterId,
            @Valid @RequestBody CreatePayrollRequest request) {
        if (requesterId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(payrollService.createPayroll(request));
    }

    @GetMapping
    public ResponseEntity<List<PayrollResponse>> getPayrolls(
            @RequestParam(required = false) PayrollStatus status,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(payrollService.getPayrolls(status, userId, startDate, endDate));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PayrollResponse> getPayrollById(@PathVariable UUID id) {
        return ResponseEntity.ok(payrollService.getPayrollById(id));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<PayrollResponse> approvePayroll(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        if (!RequestAuthorization.isAdmin(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(payrollService.approvePayroll(id));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<PayrollResponse> rejectPayroll(
            @PathVariable UUID id,
            @Valid @RequestBody RejectPayrollRequest request) {
        return ResponseEntity.ok(payrollService.rejectPayroll(id, request));
    }
}
