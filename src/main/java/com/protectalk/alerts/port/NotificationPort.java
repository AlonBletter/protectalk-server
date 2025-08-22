package com.protectalk.alerts.port;

import com.google.firebase.messaging.FirebaseMessagingException;

import java.util.List;
import java.util.Map;

public interface NotificationPort {
    record ComposedMessage(
        String title,
        String body,
        Map<String, String> data,
        List<String> tokens
    ) {}

    NotificationResult send(ComposedMessage m) throws FirebaseMessagingException;
    public record NotificationResult(
        int total,
        int success,
        int failure,
        List<Delivery> deliveries,       // aligned with tokens
        List<String> invalidTokens       // e.g., UNREGISTERED → caller can delete
    ) {}

    public record Delivery(
        String token,
        boolean success,
        String messageId,                // null on failure
        String errorCode,                // e.g., UNREGISTERED, INVALID_ARGUMENT
        String errorMessage
    ) {}
}
