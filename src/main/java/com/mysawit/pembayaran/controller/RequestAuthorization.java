package com.mysawit.pembayaran.controller;

final class RequestAuthorization {

    private static final String ADMIN_ROLE = "ADMIN";

    private RequestAuthorization() {
    }

    static boolean isAdmin(String userRole) {
        return ADMIN_ROLE.equals(userRole);
    }
}
