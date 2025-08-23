package com.protectalk.messaging;

public record Delivery(
    String token,
    boolean success,
    String messageId,
    String errorCode,
    String errorMessage
) {}
