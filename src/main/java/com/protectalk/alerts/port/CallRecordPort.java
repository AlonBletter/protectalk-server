package com.protectalk.alerts.port;

import com.protectalk.alerts.domain.AlertRecord;
import com.protectalk.alerts.domain.RiskLevel;
import com.protectalk.alerts.dto.ScamAlertRequestDto;

import java.util.List;
import java.util.Optional;

public interface CallRecordPort {
    Optional<CallRecordView> findByEventId(String eventId);
    SavedCallRecord saveHighRisk(String userId, AlertRecord req);
    void attachMessageId(String callId, String messageId);

    record SavedCallRecord(String id, RiskLevel risk, String callerNumber) {}
    record CallRecordView(String id, String messageId) {}
}