package com.protectalk.device.api;

import com.protectalk.device.dto.DeviceTokenRequestDto;
import com.protectalk.device.service.DeviceTokenService;
import com.protectalk.security.model.FirebasePrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/device-tokens")
@RequiredArgsConstructor
public class DeviceTokenController {
    private final DeviceTokenService deviceTokenService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@AuthenticationPrincipal FirebasePrincipal me,
                         @Valid @RequestBody DeviceTokenRequestDto request) {
        log.info("Device token registration request from UID: {} for device: {} on platform: {}",
                me.uid(), request.getDeviceId(), request.getPlatform());
        try {
            deviceTokenService.register(me.uid(), request);
            log.info("Device token registered successfully for UID: {} on device: {}",
                    me.uid(), request.getDeviceId());
            return ResponseEntity.ok("Device token registered successfully");
        } catch (Exception e) {
            log.error("Failed to register device token for UID: {} on device: {}",
                     me.uid(), request.getDeviceId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
}