package com.protectalk.alerts.infra;

import com.protectalk.alerts.domain.AlertRecord;
import com.protectalk.alerts.dto.ScamAlertRequestDto;
import com.protectalk.alerts.port.CallRecordPort;
import com.protectalk.db.mongo.repository.CallRecordRepository;

import java.util.Optional;

public class MongoCallRecordAdapter {
    private final CallRecordPort callRecordPort;

    public MongoCallRecordAdapter(CallRecordPort callRecordPort) {
        this.callRecordPort = callRecordPort;
    }

    public Optional<CallRecordPort.CallRecordView> findByEventId(String eventId) {
        return callRecordPort.findByEventId(eventId)
                   .map(record -> new CallRecordPort.CallRecordView(record.getId(), record.getMessageId()));
    }
    public CallRecordPort.SavedCallRecord saveHighRisk(String userId, AlertRecord req) {
        var saved = callRecordPort.save(req);
        return new CallRecordPort.SavedCallRecord(saved.getId(), saved.getRiskLevel(), saved.getCallerNumber());
    }
    public void attachMessageId(String callId, String messageId) {
        var record = callRecordPort.findById(callId)
                         .orElseThrow(() -> new IllegalArgumentException("Call record not found: " + callId));
        record.setMessageId(messageId);
        callRecordPort.save(record);
    }
}