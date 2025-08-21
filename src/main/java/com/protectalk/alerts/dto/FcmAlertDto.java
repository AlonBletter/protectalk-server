package com.protectalk.alerts.dto;

import com.protectalk.alerts.domain.AlertRecord;
import com.protectalk.alerts.domain.RiskLevel;
import lombok.Data;

import java.time.Instant;

@Data
public class FcmAlertDto {
    String    callerNumber;
    double    score;
    RiskLevel riskLevel;
    Instant   occurredAt;
    int       durationInSeconds;

    public static FcmAlertDto from(AlertRecord dto) {
        var r = new FcmAlertDto();
        r.callerNumber = dto.getCallerNumber();
        r.score = dto.getScore();
        r.riskLevel = dto.getRiskLevel();
        r.occurredAt = dto.getOccurredAt();
        r.durationInSeconds = dto.getDurationInSeconds();
        return r;
    }
}