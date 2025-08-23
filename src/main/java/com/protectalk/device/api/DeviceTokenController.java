package com.protectalk.device.api;

import com.protectalk.device.service.DeviceTokenService;
import com.protectalk.device.model.DeviceTokenEntity;
import com.protectalk.security.model.FirebasePrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/device-tokens")
public class DeviceTokenController {
    private final DeviceTokenService deviceTokenService;

    public DeviceTokenController(DeviceTokenService service) {
        this.deviceTokenService = service;
    }

    @PostMapping("/register")
    public void register(@AuthenticationPrincipal FirebasePrincipal me,
                         @RequestBody DeviceTokenEntity deviceTokenEntity) {
        // TODO make a request/response dto and convert it
        deviceTokenService.register(deviceTokenEntity);
    }
}