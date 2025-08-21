package com.protectalk.alerts.port;

import com.protectalk.alerts.domain.DeviceToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenPort extends MongoRepository<DeviceToken, String> {
    List<DeviceToken> findByUserId(String userId);
    Optional<DeviceToken> findByUserIdAndDeviceId(String userId, String deviceId);
    Optional<DeviceToken> findByFcmToken(String fcmToken);
    void deleteByFcmToken(String fcmToken);
    void deleteByUserIdAndDeviceId(String userId, String deviceId);
}