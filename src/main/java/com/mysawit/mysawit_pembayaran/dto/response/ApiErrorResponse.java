package com.mysawit.mysawit_pembayaran.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiErrorResponse {
    private int status;
    private String message;
}