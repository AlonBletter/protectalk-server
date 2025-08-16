package com.protectalk.alerts.dto;

public record ScamAlertResponseDto(
        String callId,           // Mongo _id
        String messageId,        // FCM message id (if sent)
        boolean notified         // did we notify anyone?
) {}