package com.protectalk.alerts.domain;

import com.protectalk.alerts.dto.ScamAlertRequestDto;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document("alert_records")
@NoArgsConstructor
public class AlertRecord {
    @Id
    String eventId;          // client-generated UUID for idempotency
    String callerNumber;     // E.164
    double score;      // model modelScore used by client
    RiskLevel riskLevel;     // client’s classification (expect RED or >= threshold)
    String transcript;      // optional (or hash/summary)
    Instant occurredAt;       // when the call happened
    int durationInSeconds;

    public static AlertRecord from(ScamAlertRequestDto dto) {
        var r = new AlertRecord();
        r.setEventId(dto.eventId());
        r.setCallerNumber(dto.callerNumber());
        r.setScore(dto.modelScore());
        r.setRiskLevel(dto.riskLevel());
        r.setTranscript(dto.transcript());
        r.setOccurredAt(dto.occurredAt());
        r.setDurationInSeconds(dto.durationInSeconds());
        return r;
    }
}