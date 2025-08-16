package com.protectalk.messaging.dto;

public record NotificationResponseDto(
        boolean success,
        String messageId,
        String error
) {}
