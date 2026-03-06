package com.mysawit.mysawit_pembayaran.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WalletResponse {
    private String id;
    private String userId;
    private BigDecimal balance;
}