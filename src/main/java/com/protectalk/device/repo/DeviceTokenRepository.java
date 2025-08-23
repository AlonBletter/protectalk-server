package com.protectalk.device.repo;

import com.protectalk.device.model.DeviceTokenEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends MongoRepository<DeviceTokenEntity, String> {
    List<DeviceTokenEntity> findByUserId(String userId);
    Optional<DeviceTokenEntity> findByUserIdAndDeviceId(String userId, String deviceId);
    Optional<DeviceTokenEntity> findByFcmToken(String fcmToken);
    void deleteByFcmToken(String fcmToken);
    void deleteByUserIdAndDeviceId(String userId, String deviceId);
}
