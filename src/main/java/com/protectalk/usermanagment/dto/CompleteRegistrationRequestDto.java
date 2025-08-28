package com.protectalk.usermanagment.dto;
import com.protectalk.device.dto.DeviceTokenRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CompleteRegistrationRequestDto(
        @NotBlank String name,
        @NotBlank String phoneNumber,
        @Valid @NotNull DeviceTokenRequestDto registerTokenRequest
) {}

