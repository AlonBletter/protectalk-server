package com.protectalk.alerts.infra;

import com.protectalk.alerts.domain.DeviceToken;
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
                      .orElseGet(DeviceToken::new);

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
                   .map(DeviceToken::getFcmToken)
                   .toList();
    }

    public void deleteByToken(String fcmToken) {
        repo.deleteByFcmToken(fcmToken);
    }

    public void saveOrUpdate(DeviceToken deviceToken) {
        var existing = repo.findById(deviceToken.getId());
        if (existing.isPresent()) {
            DeviceToken existingToken = existing.get();
            existingToken.setFcmToken(deviceToken.getFcmToken());
            existingToken.setPlatform(deviceToken.getPlatform());
            existingToken.setAppVersion(deviceToken.getAppVersion());
            existingToken.setLastUpdated(deviceToken.getLastUpdated());
            repo.save(existingToken);
        } else {
            // If not found, treat as new
            repo.save(deviceToken);
        }
    }
}