package com.protectalk.alerts.dto;

public record ScamAlertResponseDto(
    String callId, // Mongo ID
    String alertId, // firebase ID
    boolean notified,
    ScamAlertReason reason,
    Integer recipients,        // nullable to keep payload small
    Integer notifiedCount,
    Integer invalidTokenCount
) {
    public static ScamAlertResponseDto ok(String callId, String alertId, int recipients, int notifiedCount, int invalids) {
        return new ScamAlertResponseDto(callId, alertId, true, ScamAlertReason.OK, recipients, notifiedCount, invalids);
    }
    public static ScamAlertResponseDto belowThreshold(String callId) {
        return new ScamAlertResponseDto(callId, null, false, ScamAlertReason.BELOW_THRESHOLD, 0, 0, 0);
    }
    public static ScamAlertResponseDto noContacts(String callId, String alertId) {
        return new ScamAlertResponseDto(callId, alertId, false, ScamAlertReason.NO_CONTACTS, 0, 0, 0);
    }
    public static ScamAlertResponseDto deliveryFailed(String callId, String alertId, int recipients, int invalids) {
        return new ScamAlertResponseDto(callId, alertId, false, ScamAlertReason.DELIVERY_FAILED, recipients, 0, invalids);
    }
    public static ScamAlertResponseDto partial(String callId, String alertId, int recipients, int notifiedCount, int invalids) {
        return new ScamAlertResponseDto(callId, alertId, true, ScamAlertReason.PARTIAL_DELIVERY, recipients, notifiedCount, invalids);
    }
}
