package com.mysawit.pembayaran.client;

import com.mysawit.pembayaran.dto.response.UserSummaryResponse;
import com.mysawit.pembayaran.model.enums.UserRole;

import java.util.List;
import java.util.UUID;

/**
 * Reads user identity/role data from the User/Auth module.
 * Used to populate the admin payroll-recipient dropdown and to auto-resolve
 * a recipient's role when an admin creates a payroll.
 */
public interface UserDirectoryClient {

    /** Users an admin may create a payroll for (ADMIN accounts are excluded). */
    List<UserSummaryResponse> listSelectableUsers();

    /** Resolves the role of a single user, used to auto-fill payroll role. */
    UserRole resolveRole(UUID userId);
}
