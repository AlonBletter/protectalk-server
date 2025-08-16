package com.protectalk.alerts.service;

package com.protectalk.alerts;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeviceTokenService {

    private final DeviceTokenRepository tokenRepository;

    public DeviceTokenService(DeviceTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    /**
     * Save or update the FCM token for a given user & device.
     */
    public void saveToken(String userId, String deviceId, String fcmToken) {
        // upsert: if device already exists, update token
        tokenRepository.saveOrUpdate(userId, deviceId, fcmToken);
    }

    /**
     * Get all active tokens for a user (fan-out to multiple devices).
     */
    public List<String> getTokensForUser(String userId) {
        return tokenRepository.findTokensByUserId(userId);
    }

    /**
     * Delete an invalid/expired token (after FCM says NotRegistered).
     */
    public void deleteToken(String fcmToken) {
        tokenRepository.deleteByToken(fcmToken);
    }
}

