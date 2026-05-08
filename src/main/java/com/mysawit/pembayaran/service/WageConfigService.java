package com.mysawit.pembayaran.service;

import com.mysawit.pembayaran.dto.request.UpdateWageConfigRequest;
import com.mysawit.pembayaran.dto.response.WageConfigResponse;

public interface WageConfigService {

    WageConfigResponse getWageConfig();

    WageConfigResponse updateWageConfig(UpdateWageConfigRequest request);
}
