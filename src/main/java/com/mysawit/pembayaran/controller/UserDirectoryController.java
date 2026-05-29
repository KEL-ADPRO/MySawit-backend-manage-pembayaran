package com.mysawit.pembayaran.controller;

import com.mysawit.pembayaran.client.UserDirectoryClient;
import com.mysawit.pembayaran.dto.response.UserSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exposes the list of users an admin may pick as a payroll recipient.
 * Backs the "select recipient" dropdown on the create-payroll screen.
 */
@RestController
@RequestMapping("/api/pembayaran/users")
@RequiredArgsConstructor
public class UserDirectoryController {

    private final UserDirectoryClient userDirectoryClient;

    @GetMapping
    public ResponseEntity<List<UserSummaryResponse>> listSelectableUsers() {
        return ResponseEntity.ok(userDirectoryClient.listSelectableUsers());
    }
}
