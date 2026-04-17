package com.mysawit.pembayaran.controller;

import com.mysawit.pembayaran.dto.request.UpdateWageConfigRequest;
import com.mysawit.pembayaran.dto.response.WageConfigResponse;
import com.mysawit.pembayaran.service.WageConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pembayaran/wage-config")
@RequiredArgsConstructor
public class WageConfigController {

    private final WageConfigService wageConfigService;

    @GetMapping
    public ResponseEntity<WageConfigResponse> getWageConfig() {
        return ResponseEntity.ok(wageConfigService.getWageConfig());
    }

    @PutMapping
    public ResponseEntity<WageConfigResponse> updateWageConfig(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @Valid @RequestBody UpdateWageConfigRequest request) {
        if (!isAdmin(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(wageConfigService.updateWageConfig(request));
    }

    private boolean isAdmin(String userRole) {
        return "ADMIN".equals(userRole);
    }
}
