package com.protectalk.alert.service;

import com.protectalk.alert.dto.ScamAlertRequestDto;
import com.protectalk.alert.dto.ScamAlertResponseDto;
import com.protectalk.alert.model.AlertRecordEntity;
import com.protectalk.alert.model.RiskLevel;
import com.protectalk.alert.repo.ScamAlertRepository;
import com.protectalk.messaging.NotificationComposer;

import com.protectalk.device.service.DeviceTokenService;
import com.protectalk.messaging.NotificationGateway;
import com.protectalk.messaging.NotificationResult;
import com.protectalk.usermanagment.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class ScamAlertService {
    private static final Logger log = LoggerFactory.getLogger(ScamAlertService.class);

    private final double               minModelScore;
    private final ScamAlertRepository  scamAlertRepository;  // persistence
    private final DeviceTokenService   deviceTokenService;    // token resolution & cleanup
    private final UserService          userService;
    private final NotificationGateway  notifierGateway;        // FCM (or other) adapter
    private final NotificationComposer notificationComposer;        // builds notification payload/message

    public ScamAlertService(@Value("${protectalk.threshold.modelScore:0.75}") double minModelScore,
                            ScamAlertRepository scamAlertRepository, DeviceTokenService deviceTokenService,
                            NotificationGateway notifier, UserService userService, NotificationGateway notifierGateway, NotificationComposer notificationComposer) {
        this.minModelScore = minModelScore; // immutable after wiring
        this.scamAlertRepository = scamAlertRepository;
        this.deviceTokenService = deviceTokenService;
        this.userService = userService;
        this.notificationComposer = notificationComposer;
        this.notifierGateway = notifierGateway;
    }

    private boolean isModelScorePassesThreshold(double score, RiskLevel level) {
        return level == RiskLevel.RED || score >= minModelScore;
    }

    public ScamAlertResponseDto handle(String userId, ScamAlertRequestDto req) throws Exception {
        log.info("Processing scam alert - UID: {} eventId: {} caller: {} risk: {} score: {}",
                userId, req.eventId(), req.callerNumber(), req.riskLevel(), req.modelScore());

        // 0) Idempotency: if this event was already processed, short-circuit
        var alertRecordEntity = scamAlertRepository.findByUserIdAndEventId(userId, req.eventId());
        if (alertRecordEntity.isPresent()) {
            log.info("Duplicate alert request ignored - UID: {} eventId: {} existing alertId: {}",
                    userId, req.eventId(), alertRecordEntity.get().getId());
            return ScamAlertResponseDto.ok(alertRecordEntity.get().getId(), 0, 0, 0);
        }

        // 1) Threshold check — not an error, just "not propagated"
        if (!isModelScorePassesThreshold(req.modelScore(), req.riskLevel())) {
            log.info("Alert below threshold - UID: {} eventId: {} score: {} level: {} threshold: {}",
                    userId, req.eventId(), req.modelScore(), req.riskLevel(), minModelScore);

            // Still save the record for analytics, but don't notify
            var savedRecord = scamAlertRepository.save(AlertRecordEntity.from(userId, req));
            return ScamAlertResponseDto.belowThreshold(savedRecord.getId());
        }

        // 2) Persist the high-risk record (returns saved call record)
        var savedAlertRecordEntity = scamAlertRepository.save(AlertRecordEntity.from(userId, req));
        log.debug("Alert record saved - alertId: {} for UID: {} eventId: {}",
                savedAlertRecordEntity.getId(), userId, req.eventId());

        // 3) Resolve targets (trusted contacts + owner if desired) -> list of FCM tokens
        List<String> tokens = userService.getTrustedContactTokens(userId);
        if (tokens == null || tokens.isEmpty()) {
            log.warn("No trusted contacts found for notifications - UID: {} alertId: {}",
                    userId, savedAlertRecordEntity.getId());
            return ScamAlertResponseDto.noContacts(savedAlertRecordEntity.getId());
        }

        log.info("Sending alert notifications - UID: {} alertId: {} recipients: {}",
                userId, savedAlertRecordEntity.getId(), tokens.size());

        // 4) Compose + send
        var message = notificationComposer.compose(savedAlertRecordEntity, tokens);
        var result = notifierGateway.send(message);

        // Prefer success/total from provider (authoritative)
        int recipients   = result.total();
        int delivered    = result.success();
        var invalids     = result.invalidTokens() == null ? List.<String>of() : result.invalidTokens();
        int invalidCount = invalids.size();

        logMessageResult(req, recipients, result, delivered, savedAlertRecordEntity, invalidCount);

        // 5) Cleanup invalid tokens reported by the provider
        if (!invalids.isEmpty()) {
            log.info("Cleaning up {} invalid FCM tokens for UID: {}", invalidCount, userId);
            invalids.forEach(deviceTokenService::deleteToken);
        }

        // 6) Build accurate response
        if (delivered == 0 && invalidCount > 0) {
            log.warn("Alert delivery completely failed due to invalid tokens - UID: {} alertId: {} invalid: {}",
                    userId, savedAlertRecordEntity.getId(), invalidCount);
            return ScamAlertResponseDto.deliveryFailed(savedAlertRecordEntity.getId(), recipients, invalidCount);
        }
        if (invalidCount > 0 || delivered < recipients) {
            log.warn("Alert delivery partially failed - UID: {} alertId: {} delivered: {}/{} invalid: {}",
                    userId, savedAlertRecordEntity.getId(), delivered, recipients, invalidCount);
            return ScamAlertResponseDto.partial(savedAlertRecordEntity.getId(), recipients, delivered, invalidCount);
        }

        log.info("Alert delivery successful - UID: {} alertId: {} delivered: {}/{}",
                userId, savedAlertRecordEntity.getId(), delivered, recipients);
        return ScamAlertResponseDto.ok(savedAlertRecordEntity.getId(), recipients, delivered, invalidCount);
    }

    private static void logMessageResult(ScamAlertRequestDto req, int recipients, NotificationResult result, int delivered,
                                  AlertRecordEntity savedAlertRecordEntity, int invalidCount) {
        // build a compact reference (single → real id, multi → "batch:S/T")
        String sendRef = (recipients == 1 && !result.deliveries().isEmpty()) ? result.deliveries().get(0).messageId() :
                         "batch:%d/%d".formatted(delivered, recipients);

        // --- LOGS (replace the repository attach step with these) ---
        log.info("ALERT_DELIVERY summary alertId={} eventId={} recipients={} delivered={} invalid={} ref={}",
                 savedAlertRecordEntity.getId(), req.eventId(), recipients, delivered, invalidCount, sendRef);
    }
}