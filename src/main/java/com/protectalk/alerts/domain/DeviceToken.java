package com.protectalk.alerts.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Represents a single device's push token for a user.
 * - Unique by (userId, deviceId) for easy upsert.
 * - Unique by fcmToken to avoid duplicates across users/devices.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "device_tokens")
@CompoundIndex(name = "uid_device_unique", def = "{'userId': 1, 'deviceId': 1}", unique = true)
public class DeviceToken {

    @Id
    private String id;

    /** UID from Firebase Auth (phone sign-in in your app). */
    @Indexed
    private String userId;

    /** Client-generated stable identifier for the device (e.g., ANDROID_ID or your own UUID). */
    private String deviceId;

    /** Current FCM registration token for this device installation. */
    @Indexed(unique = true)
    private String fcmToken;

    /** Platform hint: "android", "ios", "web" (optional but handy). */
    private Platform platform;

    /** Client app version that registered this token (useful for debugging/rollouts). */
    private String appVersion;

    /** Epoch millis when this token record was last updated (server time). */
    private long lastUpdated;
}
