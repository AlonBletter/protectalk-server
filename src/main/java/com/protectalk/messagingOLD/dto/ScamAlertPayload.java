package com.protectalk.messagingOLD.dto;

public record ScamAlertPayload(
        String callId,
        String riskLevel,     // "GREEN", "YELLOW", "RED"
        String phoneNumber,
        String modelAnalysis
) {}
