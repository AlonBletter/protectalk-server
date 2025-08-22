package com.protectalk.alerts.port;

import com.protectalk.alerts.domain.AlertRecord;
import com.protectalk.alerts.domain.RiskLevel;
import com.protectalk.alerts.dto.ScamAlertRequestDto;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CallRecordPort extends MongoRepository<AlertRecord, String> {
    Optional<AlertRecord> findByEventId(String eventId);
    void saveHighRisk(String userId, AlertRecord req);
    void attachMessageId(String callId, String messageId);
}