package com.protectalk.alerts.port;

import com.protectalk.db.model.DeviceTokenEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenPort extends MongoRepository<DeviceTokenEntity, String> {
    List<DeviceTokenEntity> findByUserId(String userId);
    Optional<DeviceTokenEntity> findByUserIdAndDeviceId(String userId, String deviceId);
    Optional<DeviceTokenEntity> findByFcmToken(String fcmToken);
    void deleteByFcmToken(String fcmToken);
    void deleteByUserIdAndDeviceId(String userId, String deviceId);
}