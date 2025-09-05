// src/main/java/com/protectalk/alerts/model/CallRecordDocument.java
package com.protectalk.alert.model;

import com.protectalk.alert.dto.ScamAlertRequestDto;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("call_records")
@CompoundIndex(name = "uk_user_event", def = "{'userId':1,'eventId':1}", unique = true)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AlertRecordEntity {

    @Id
    private String id;

    @Indexed
    private String userId;          // firebase owner (UID)

    @Indexed
    private String eventId;         // optional external event/call id

    private String callerNumber;
    private RiskLevel riskLevel;
    private String transcript;
    private double modelScore;
    private String modelAnalysis;
    private int durationInSeconds;

    private Instant occurredAt;             // when the call happened
    private String messageId;       // FCM message ref (set after send)

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    private Instant deletedAt;     // soft delete marker

    public static AlertRecordEntity from(String userId, ScamAlertRequestDto req) {
        AlertRecordEntity entity = new AlertRecordEntity();
        entity.setEventId(req.eventId());
        entity.setUserId(userId);
        entity.setCallerNumber(req.callerNumber());
        entity.setRiskLevel(req.riskLevel());
        entity.setModelScore(req.modelScore());
        entity.setTranscript(req.transcript());
        entity.setModelAnalysis(req.modelAnalysis());
        entity.setOccurredAt(req.occurredAt());
        entity.setDurationInSeconds(req.durationInSeconds());
        return entity;
    }
}
