package com.protectalk.alerts.api;

import com.protectalk.alerts.service.DeviceTokenService;
import com.protectalk.alerts.domain.DeviceToken;
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
                         @RequestBody DeviceToken deviceToken) {
        deviceTokenService.register(deviceToken);
    }

//    @DeleteMapping("/{token}")
//    public void delete(@PathVariable String token) {
//        service.removeInvalid(token);
//    }
}