package com.protectalk.messaging.service;

import com.protectalk.messaging.dto.NotificationRequestDto;
import com.protectalk.messaging.dto.ScamAlertPayload;
import com.protectalk.messaging.event.ScamAlertEvent;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationRequestDto toRequest(ScamAlertEvent event, String targetToken) {
        return new NotificationRequestDto(
                targetToken,
                "Scam Alert: " + event.riskLevel(),
                "Possible scam from " + event.phoneNumber(),
                new ScamAlertPayload(event.callId(), event.riskLevel(), event.phoneNumber(), event.modelAnalysis())
        );
    }
}
