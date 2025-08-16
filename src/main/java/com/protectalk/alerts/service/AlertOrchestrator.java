package com.protectalk.alerts.service;

import com.protectalk.alerts.dto.ScamAlertRequestDto;
import com.protectalk.alerts.dto.ScamAlertResponseDto;
import com.protectalk.alerts.port.CallRecordPort;
import com.protectalk.alerts.port.NotificationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AlertOrchestrator {
    private final ThresholdGate gate;            // still validate server-side
    private final CallRecordPort callRecordPort; // persistence
    private final DeviceTokenService deviceTokens;
    private final NotificationPort notifier;
    private final NotificationComposer composer;

    public ScamAlertResponseDto handle(String userId, ScamAlertRequestDto req) {
        // idempotency short-circuit (returns existing callId if already processed)
        var existing = callRecordPort.findByEventId(req.eventId());
        if (existing.isPresent()) {
            return new ScamAlertResponseDto(existing.get().id(), existing.get().messageId(), existing.get().messageId()!=null);
        }

        if (!gate.allows(req.score(), req.riskLevel())) {
            // do nothing (or store a lightweight audit row), but don’t notify
            return new ScamAlertResponseDto(null, null, false);
        }

        // 1) persist the call record (returns callId)
        var saved = callRecordPort.saveHighRisk(userId, req);

        // 2) resolve targets (owner + trusted contacts)
        var targets = deviceTokens.resolveTargetsForUser(userId); // returns List<String> fcmTokens
        if (targets.isEmpty()) {
            return new ScamAlertResponseDto(saved.id(), null, false);
        }

        // 3) compose + send
        var msg = composer.compose(saved, targets);              // title/body + data(callId,risk,phone)
        var messageId = notifier.send(msg);

        // 4) attach message id to record (for traceability)
        callRecordPort.attachMessageId(saved.id(), messageId);

        return new ScamAlertResponseDto(saved.id(), messageId, true);
    }
}
