package com.protectalk.device.service;

import com.protectalk.device.dto.DeviceTokenRequestDto;
import com.protectalk.device.model.DeviceTokenEntity;
import com.protectalk.device.repo.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository repo;

    /**
     * Save or update the FCM token for a given user & device using DTO.
     */
    public void register(String userId, DeviceTokenRequestDto request) {
        log.info("Registering device token for UID: {} on {} platform", userId, request.getPlatform());

        DeviceTokenEntity entity = DeviceTokenEntity.builder()
                .userId(userId)
                .deviceId(request.getDeviceId())
                .fcmToken(request.getFcmToken())
                .platform(request.getPlatform())
                .appVersion(request.getAppVersion())
                .lastUpdated(System.currentTimeMillis())
                .build();

        register(entity);
        log.info("Successfully registered device token for UID: {} on device: {}", userId, request.getDeviceId());
    }

    /**
     * Save or update the FCM token for a given user & device.
     * - If id is provided, update by id (or insert if missing).
     * - Else, match by (userId, deviceId) and update; if not found, insert.
     * Handles rare duplicate-key races by re-reading and updating.
     */
    public void register(DeviceTokenEntity input) {
        if (input == null) throw new IllegalArgumentException("device token entity is required");

        log.debug("Registering device token entity for UID: {} with deviceId: {}", input.getUserId(), input.getDeviceId());

        // 1) If _id is known, prefer update-by-id
        if (input.getId() != null) {
            repo.findById(input.getId()).ifPresentOrElse(existing -> {
                log.debug("Updating existing device token by ID: {}", input.getId());
                copyMutableFields(input, existing);
                repo.save(existing);
            }, () -> {
                log.debug("Inserting new device token with ID: {}", input.getId());
                repo.save(input);
            });
            return;
        }

        // 2) No _id given, try to match by (userId, deviceId)
        var existing = repo.findByUserIdAndDeviceId(input.getUserId(), input.getDeviceId());
        if (existing.isPresent()) {
            log.debug("Updating existing device token for UID: {} and deviceId: {}", input.getUserId(), input.getDeviceId());
            var entity = existing.get();
            copyMutableFields(input, entity);
            repo.save(entity);
            return;
        }

        // 3) Not found, try insert
        try {
            log.debug("Inserting new device token for UID: {} and deviceId: {}", input.getUserId(), input.getDeviceId());
            repo.save(input);
        } catch (DuplicateKeyException ex) {
            log.warn("Duplicate key exception during insert, retrying with find and update for UID: {} and deviceId: {}",
                    input.getUserId(), input.getDeviceId());
            // Race condition: another thread inserted it, re-read and update
            var nowExisting = repo.findByUserIdAndDeviceId(input.getUserId(), input.getDeviceId());
            if (nowExisting.isPresent()) {
                var entity = nowExisting.get();
                copyMutableFields(input, entity);
                repo.save(entity);
            } else {
                log.error("Failed to find device token after duplicate key exception for UID: {} and deviceId: {}",
                         input.getUserId(), input.getDeviceId());
                throw new RuntimeException("Could not insert or update after duplicate key exception", ex);
            }
        }
    }

    /**
     * Get all active tokens for a user (fan-out to multiple devices).
     */
    public List<String> getTrustedContactsTokensForUser(String userId) {
        return repo.findByUserId(userId).stream()
                   .map(DeviceTokenEntity::getFcmToken)
                   .toList();
    }

    /**
     * Delete an invalid/expired token (after FCM says UNREGISTERED / INVALID_ARGUMENT).
     */
    public void deleteToken(String fcmToken) {
        repo.deleteByFcmToken(fcmToken);
    }

    // --- helpers ---

    private static void copyMutableFields(DeviceTokenEntity src, DeviceTokenEntity dst) {
        dst.setFcmToken(src.getFcmToken());
        dst.setPlatform(src.getPlatform());
        dst.setAppVersion(src.getAppVersion());
        dst.setLastUpdated(src.getLastUpdated());
        // If you have updatedAt/createdAt fields, set them here as needed.
    }
}
