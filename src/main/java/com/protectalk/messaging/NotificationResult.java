package com.protectalk.messaging;

import java.util.List;

public record NotificationResult(
    int total,
    int success,
    int failure,
    List<Delivery> deliveries,
    List<String> invalidTokens
) {}
