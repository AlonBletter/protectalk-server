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
    @NotBlank
    private String deviceId;
    @NotBlank
    private String fcmToken;
    @NotNull
    private Platform platform;
    private String appVersion;
}
