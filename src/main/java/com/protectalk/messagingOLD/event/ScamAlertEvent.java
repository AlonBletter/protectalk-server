package com.protectalk.messagingOLD.event;

public record ScamAlertEvent(String callId, String phoneNumber, String riskLevel, String modelAnalysis) {
}
