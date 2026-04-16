package com.mysawit.pembayaran.service;

import com.mysawit.pembayaran.dto.request.UpdateWageConfigRequest;
import com.mysawit.pembayaran.dto.response.WageConfigResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WageConfigServiceImpl implements WageConfigService {

    @Override
    public WageConfigResponse getWageConfig() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public WageConfigResponse updateWageConfig(UpdateWageConfigRequest request) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
