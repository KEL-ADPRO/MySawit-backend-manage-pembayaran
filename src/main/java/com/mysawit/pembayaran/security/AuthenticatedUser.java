package com.mysawit.pembayaran.security;

import com.mysawit.pembayaran.model.enums.UserRole;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, UserRole role) {

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}
