package com.mysawit.pembayaran.dto.response;

import com.mysawit.pembayaran.model.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserSummaryResponse {
    private UUID id;
    private String nama;
    private String email;
    private UserRole role;
}
