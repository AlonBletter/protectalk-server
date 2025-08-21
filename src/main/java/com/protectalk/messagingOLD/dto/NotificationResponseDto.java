package com.protectalk.messagingOLD.dto;

public record NotificationResponseDto(
        boolean success,
        String messageId,
        String error
) {}
