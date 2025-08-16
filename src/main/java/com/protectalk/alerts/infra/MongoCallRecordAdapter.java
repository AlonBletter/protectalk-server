package com.protectalk.alerts.infra;

import com.protectalk.alerts.dto.ScamAlertRequestDto;
import com.protectalk.alerts.port.CallRecordPort;
import com.protectalk.db.mongo.repository.CallRecordRepository;

import java.util.Optional;

public class MongoCallRecordAdapter implements CallRecordPort {
    private final CallRecordRepository repo;

    public Optional<CallRecordView> findByEventId(String eventId) { /* repo lookup */ }
    public SavedCallRecord saveHighRisk(String userId, ScamAlertRequestDto req) { /* map & repo.save */ }
    public void attachMessageId(String callId, String messageId) { /* update */ }
}