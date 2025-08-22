package com.protectalk.alerts.infra;

import com.protectalk.db.model.DeviceTokenEntity;
import com.protectalk.alerts.domain.Platform;
import com.protectalk.alerts.port.DeviceTokenPort;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DeviceTokenAdapter {

    private final DeviceTokenPort repo;

    public DeviceTokenAdapter(DeviceTokenPort repo) {
        this.repo = repo;
    }

    /**
     * Upsert by (userId, deviceId).
     * Ensures the latest token/platform/version/lastUpdated are stored.
     */
    public void upsert(String userId,
                       String deviceId,
                       String fcmToken,
                       Platform platform,
                       String appVersion,
                       long lastUpdatedEpochMs) {

        var doc = repo.findByUserIdAndDeviceId(userId, deviceId)
                      .orElseGet(DeviceTokenEntity::new);

        doc.setUserId(userId);
        doc.setDeviceId(deviceId);
        doc.setFcmToken(fcmToken);
        doc.setPlatform(platform);
        doc.setAppVersion(appVersion);
        doc.setLastUpdated(lastUpdatedEpochMs);

        repo.save(doc);
    }

    /** Convenience: only the token strings for fan-out sends. */
    public List<String> findTokensByUserId(String userId) {
        return repo.findByUserId(userId)
                   .stream()
                   .map(DeviceTokenEntity::getFcmToken)
                   .toList();
    }

    public void deleteByToken(String fcmToken) {
        repo.deleteByFcmToken(fcmToken);
    }

    public void saveOrUpdate(DeviceTokenEntity deviceTokenEntity) {
        var existing = repo.findById(deviceTokenEntity.getId());
        if (existing.isPresent()) {
            DeviceTokenEntity existingToken = existing.get();
            existingToken.setFcmToken(deviceTokenEntity.getFcmToken());
            existingToken.setPlatform(deviceTokenEntity.getPlatform());
            existingToken.setAppVersion(deviceTokenEntity.getAppVersion());
            existingToken.setLastUpdated(deviceTokenEntity.getLastUpdated());
            repo.save(existingToken);
        } else {
            // If not found, treat as new
            repo.save(deviceTokenEntity);
        }
    }
}