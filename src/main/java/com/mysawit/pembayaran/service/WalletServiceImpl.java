package com.mysawit.pembayaran.service;

import com.mysawit.pembayaran.dto.request.TopUpRequest;
import com.mysawit.pembayaran.dto.response.TopUpResponse;
import com.mysawit.pembayaran.dto.response.WalletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    @Override
    public WalletResponse getWalletByUserId(UUID userId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public WalletResponse createWallet(UUID userId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public TopUpResponse initiateTopUp(TopUpRequest request) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void handleTopUpCallback(Map<String, Object> payload) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
