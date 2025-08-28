package com.protectalk.device.service;

import com.protectalk.device.dto.DeviceTokenRequestDto;
import com.protectalk.device.model.DeviceTokenEntity;
import com.protectalk.device.repo.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository repo;

    /**
     * Save or update the FCM token for a given user & device using DTO.
     */
    public void register(String userId, DeviceTokenRequestDto request) {
        DeviceTokenEntity entity = DeviceTokenEntity.builder()
                .userId(userId)
                .deviceId(request.getDeviceId())
                .fcmToken(request.getFcmToken())
                .platform(request.getPlatform())
                .appVersion(request.getAppVersion())
                .lastUpdated(System.currentTimeMillis())
                .build();

        register(entity);
    }

    /**
     * Save or update the FCM token for a given user & device.
     * - If id is provided, update by id (or insert if missing).
     * - Else, match by (userId, deviceId) and update; if not found, insert.
     * Handles rare duplicate-key races by re-reading and updating.
     */
    public void register(DeviceTokenEntity input) {
        if (input == null) throw new IllegalArgumentException("device token entity is required");

        // 1) If _id is known, prefer update-by-id
        if (input.getId() != null) {
            repo.findById(input.getId()).ifPresentOrElse(existing -> {
                copyMutableFields(input, existing);
                repo.save(existing);
            }, () -> repo.save(input));
            return;
        }

        // 2) Otherwise match by (userId, deviceId)
        var existingOpt = repo.findByUserIdAndDeviceId(input.getUserId(), input.getDeviceId());
        if (existingOpt.isPresent()) {
            var existing = existingOpt.get();
            copyMutableFields(input, existing);
            repo.save(existing);
            return;
        }

        // 3) New doc; protect against race with unique indexes
        try {
            repo.save(input);
        } catch (DuplicateKeyException e) {
            // Another insert won the race → read & update
            repo.findByUserIdAndDeviceId(input.getUserId(), input.getDeviceId())
                .ifPresent(ex -> {
                    copyMutableFields(input, ex);
                    repo.save(ex);
                });
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
