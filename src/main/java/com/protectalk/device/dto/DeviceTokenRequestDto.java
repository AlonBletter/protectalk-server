// src/main/java/com/protectalk/device/dto/RegisterDeviceTokenRequest.java
package com.protectalk.device.dto;

import com.protectalk.device.model.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Request DTO for registering/updating a device's FCM token.
 * The authenticated UID comes from your auth filter; not included here.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceTokenRequestDto {

    /** Client-generated stable identifier for the device (e.g., ANDROID_ID or your UUID). */
    @NotBlank
    private String deviceId;

    /** Current FCM registration token for this device installation. */
    @NotBlank
    private String fcmToken;

    /** "android" | "ios" | "web" (enum). */
    @NotNull
    private Platform platform;

    /** App version that registered this token (useful for debugging/rollouts). */
    private String appVersion;

    /** Epoch millis when the client last refreshed this token. */
    private long lastUpdatedEpochMs;
}
