package com.protectalk.usermanagment.dto;

public record UserRequestDto(
        String phoneNumber,
        String name,
        String dateOfBirth,
        String userType,           // Protegee / TrustedContact / Both TODO enum
        String linkedContactPhone,
        String linkedContactName,
        String relationship
) {}
