package com.protectalk.usermanagment.dto;

import com.protectalk.usermanagment.model.ContactType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ContactRequestDto(
        @NotBlank String name,
        @NotBlank String phoneNumber,
        @NotBlank String relationship,
        @NotNull ContactType contactType
) {}
