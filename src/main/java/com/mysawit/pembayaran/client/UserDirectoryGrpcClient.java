package com.mysawit.pembayaran.client;

import com.mysawit.pembayaran.dto.response.UserSummaryResponse;
import com.mysawit.pembayaran.grpc.user.proto.GetUserByIdRequest;
import com.mysawit.pembayaran.grpc.user.proto.ListUsersRequest;
import com.mysawit.pembayaran.grpc.user.proto.UserServiceGrpc;
import com.mysawit.pembayaran.grpc.user.proto.UserSummary;
import com.mysawit.pembayaran.model.enums.UserRole;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class UserDirectoryGrpcClient implements UserDirectoryClient {

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userStub;

    @Override
    public List<UserSummaryResponse> listSelectableUsers() {
        try {
            return userStub.listUsers(ListUsersRequest.getDefaultInstance())
                    .getUsersList().stream()
                    .map(this::toResponse)
                    .flatMap(Optional::stream)
                    // ADMIN accounts cannot receive a payroll, so they are never selectable.
                    .filter(user -> user.getRole() != UserRole.ADMIN)
                    .toList();
        } catch (StatusRuntimeException ex) {
            throw translate(ex, "list users");
        }
    }

    @Override
    public UserRole resolveRole(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required to resolve role");
        }
        try {
            UserSummary user = userStub.getUserById(
                    GetUserByIdRequest.newBuilder().setUserId(userId.toString()).build());
            return parseRole(user.getRole())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "User " + userId + " has unsupported role: " + user.getRole()));
        } catch (StatusRuntimeException ex) {
            if (ex.getStatus().getCode() == Status.Code.NOT_FOUND) {
                throw new IllegalArgumentException("User not found: " + userId);
            }
            throw translate(ex, "resolve role for user " + userId);
        }
    }

    private Optional<UserSummaryResponse> toResponse(UserSummary user) {
        Optional<UserRole> role = parseRole(user.getRole());
        if (role.isEmpty()) {
            log.warn("Skipping user {} with unsupported role '{}'", user.getId(), user.getRole());
            return Optional.empty();
        }
        UUID id;
        try {
            id = UUID.fromString(user.getId());
        } catch (IllegalArgumentException ex) {
            log.warn("Skipping user with invalid id '{}'", user.getId());
            return Optional.empty();
        }
        return Optional.of(UserSummaryResponse.builder()
                .id(id)
                .nama(user.getNama())
                .email(user.getEmail())
                .role(role.get())
                .build());
    }

    private Optional<UserRole> parseRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UserRole.valueOf(rawRole.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private RuntimeException translate(StatusRuntimeException ex, String action) {
        log.error("gRPC call to User module failed while trying to {}: {}", action, ex.getStatus());
        return new IllegalStateException("User directory service unavailable while trying to " + action, ex);
    }
}
