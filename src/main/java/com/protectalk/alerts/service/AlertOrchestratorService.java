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
    private final CallRecordPort       callRecordPort;  // persistence
    private final DeviceTokenService   deviceTokenService;    // token resolution & cleanup
    private final NotificationPort     notifier;        // FCM (or other) adapter
    private final NotificationComposer composer;        // builds notification payload/message

    public ScamAlertResponseDto handle(String userId, ScamAlertRequestDto req) throws Exception {
        // 0) Idempotency: if this event was already processed, short-circuit
        var existing = callRecordPort.findByEventId(req.eventId());
        if (existing.isPresent()) {
            return ScamAlertResponseDto.ok(existing.get().id(), existing.get().messageId(), 0, 0, 0);
        }

        // 1) Threshold check — not an error, just "not propagated"
        if (!gate.allows(req.modelScore(), req.riskLevel())) {
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

        // 4) Compose + send
        var message = composer.compose(saved, tokens);   // include data: callId, risk, number, etc.
        var result = notifier.send(message);            // NotificationResult: total/success/failure/deliveries/invalidTokens

        // Prefer success/total from provider (authoritative)
        int recipients   = result.total();
        int delivered    = result.success();
        var invalids     = result.invalidTokens() == null ? List.<String>of() : result.invalidTokens();
        int invalidCount = invalids.size();

        // Choose a traceable ref:
        // - single: the real messageId
        // - multi : a compact "batch:S/T" (you can change to join a few IDs if you want)
        String messageRef = toMessageRef(result);

        // 5) Attach message ref for traceability
        callRecordPort.attachMessageId(saved.id(), messageRef);

        // 6) Cleanup invalid tokens reported by the provider
        if (!invalids.isEmpty()) {
            invalids.forEach(deviceTokenService::deleteToken);
        }

        // 7) Build accurate response
        if (delivered == 0 && invalidCount > 0) {
            // Everything failed due to invalid tokens (or at least none delivered)
            return ScamAlertResponseDto.deliveryFailed(saved.id(), messageRef, recipients, invalidCount);
        }
        if (invalidCount > 0 || delivered < recipients) {
            // Partial success (some failed or invalid)
            return ScamAlertResponseDto.partial(saved.id(), messageRef, recipients, delivered, invalidCount);
        }
        // All good
        return ScamAlertResponseDto.ok(saved.id(), messageRef, recipients, delivered, invalidCount);
    }

    // --- helper ---
    private static String toMessageRef(NotificationPort.NotificationResult r) {
        if (r == null) {
            return null;
        }
        if (r.total() == 1 && !r.deliveries().isEmpty()) {
            return r.deliveries().get(0).messageId(); // real ID for single send
        }

        return "batch:%d/%d".formatted(r.success(), r.total());
    }
}