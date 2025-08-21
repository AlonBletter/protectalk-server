package com.protectalk.alerts.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.protectalk.alerts.domain.AlertRecord;
import com.protectalk.alerts.dto.ScamAlertRequestDto;
import com.protectalk.alerts.dto.ScamAlertResponseDto;
import com.protectalk.alerts.port.CallRecordPort;
import com.protectalk.alerts.port.NotificationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertOrchestratorService {

    private final ThresholdGate        gate;            // server-side validation
    private final CallRecordPort     callRecordPort;  // persistence
    private final DeviceTokenService deviceTokenService;    // token resolution & cleanup
    private final NotificationPort   notifier;        // FCM (or other) adapter
    private final NotificationComposer composer;        // builds notification payload/message

    public ScamAlertResponseDto handle(String userId, ScamAlertRequestDto req) throws Exception {
        // 0) Idempotency: if this event was already processed, short-circuit
        var existing = callRecordPort.findByEventId(req.eventId());
        if (existing.isPresent()) {
            // If you store counts on the record you can surface them here instead of zeros.
            return ScamAlertResponseDto.ok(
                existing.get().id(),
                existing.get().messageId(),
                0, /* recipients */
                0, /* notifiedCount */
                0  /* invalidTokenCount */
                                          );
        }

        // 1) Threshold check — not an error, just "not propagated"
        if (!gate.allows(req.modelScore(), req.riskLevel())) {
            // Optionally persist a lightweight audit
            return ScamAlertResponseDto.belowThreshold(req.eventId());
        }

        // 2) Persist the high-risk record (returns saved call record)
        var saved = callRecordPort.saveHighRisk(userId, AlertRecord.from(req));

        // 3) Resolve targets (trusted contacts + owner if desired) -> list of FCM tokens
        List<String> tokens = deviceTokenService.getTrustedContactsTokensForUser(userId);
        if (tokens == null || tokens.isEmpty()) {
            // No tokens available to notify (e.g., no trusted contacts or no registered devices)
            return ScamAlertResponseDto.noContacts(saved.id(), null);
        }

        int recipients = tokens.size(); // here we're counting tokens; switch to unique user count if you prefer

        // 4) Compose + send
        var message  = composer.compose(saved, tokens);        // include data: callId, risk, number, etc.
        var result   = notifier.send(message);                 // result contains messageId + invalidTokens (if any)
        var messageId = result.messageId();

        // 5) Attach message id for traceability
        callRecordPort.attachMessageId(saved.id(), messageId);

        // 6) Cleanup invalid tokens reported by the provider
        var invalids = result.invalidTokens();
        if (invalids != null && !invalids.isEmpty()) {
            invalids.forEach(deviceTokenService::removeInvalid);
        }

        int invalidCount  = invalids == null ? 0 : invalids.size();
        int notifiedCount = Math.max(0, recipients - invalidCount);

        // 7) Build accurate response
        if (notifiedCount == 0 && invalidCount > 0) {
            // Everything failed due to invalid tokens
            return ScamAlertResponseDto.deliveryFailed(saved.id(), messageId, recipients, invalidCount);
        }
        if (invalidCount > 0) {
            // Some delivered, some failed/invalid → partial
            return ScamAlertResponseDto.partial(saved.id(), messageId, recipients, notifiedCount, invalidCount);
        }
        // All good
        return ScamAlertResponseDto.ok(saved.id(), messageId, recipients, notifiedCount, invalidCount);
    }
}