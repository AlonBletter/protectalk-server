package com.protectalk.messaging.event;

public record ScamAlertEvent(String callId, String phoneNumber, String riskLevel, String modelAnalysis) {
}
