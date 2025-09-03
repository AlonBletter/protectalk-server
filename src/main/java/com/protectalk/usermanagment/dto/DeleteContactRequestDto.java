package com.protectalk.usermanagment.dto;

import com.protectalk.usermanagment.model.ContactType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeleteContactRequestDto(
    @NotBlank(message = "Phone number is required")
    String phoneNumber,

    @NotNull(message = "Contact type is required")
    ContactType contactType
) {}
