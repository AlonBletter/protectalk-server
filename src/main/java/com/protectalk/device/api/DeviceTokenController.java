package com.protectalk.device.api;

import com.protectalk.device.dto.DeviceTokenRequestDto;
import com.protectalk.device.service.DeviceTokenService;
import com.protectalk.security.model.FirebasePrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/device-tokens")
@RequiredArgsConstructor
public class DeviceTokenController {
    private final DeviceTokenService deviceTokenService;

    @PostMapping("/register")
    public void register(@AuthenticationPrincipal FirebasePrincipal me,
                         @Valid @RequestBody DeviceTokenRequestDto request) {
        deviceTokenService.register(me.uid(), request);
    }
}