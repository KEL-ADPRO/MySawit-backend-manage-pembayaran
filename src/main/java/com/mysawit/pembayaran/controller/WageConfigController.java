package com.mysawit.pembayaran.controller;

import com.mysawit.pembayaran.dto.request.UpdateWageConfigRequest;
import com.mysawit.pembayaran.dto.response.WageConfigResponse;
import com.mysawit.pembayaran.service.WageConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
            @Valid @RequestBody UpdateWageConfigRequest request) {
        return ResponseEntity.ok(wageConfigService.updateWageConfig(request));
    }
}
