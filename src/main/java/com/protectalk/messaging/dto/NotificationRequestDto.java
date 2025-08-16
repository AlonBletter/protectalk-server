package com.protectalk.messaging.dto;

public record NotificationRequestDto(
        String targetToken,   // FCM device token
        String title,         // Notification title
        String body,          // Notification body
        ScamAlertPayload data // Additional data payload
) {}
