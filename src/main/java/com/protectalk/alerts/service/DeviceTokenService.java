package com.protectalk.alerts.service;

import com.protectalk.alerts.domain.DeviceToken;
import com.protectalk.alerts.infra.DeviceTokenAdapter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeviceTokenService {

    private final DeviceTokenAdapter deviceTokenAdapter;

    public DeviceTokenService(DeviceTokenAdapter tokenRepository) {
        this.deviceTokenAdapter = tokenRepository;
    }

    /**
     * Save or update the FCM token for a given user & device.
     */
    public void register(DeviceToken deviceToken) {
        deviceTokenAdapter.saveOrUpdate(deviceToken);
    }

    /**
     * Get all active tokens for a user (fan-out to multiple devices).
     */
    public List<String> getTrustedContactsTokensForUser(String userId) {
        return deviceTokenAdapter.findTokensByUserId(userId);
    }

    /**
     * Delete an invalid/expired token (after FCM says NotRegistered).
     */
    public void deleteToken(String fcmToken) {
        deviceTokenAdapter.deleteByToken(fcmToken);
    }
}

