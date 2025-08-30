package com.protectalk.usermanagment.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ContactType {
    TRUSTED_CONTACT,    // Someone who will receive alerts from me
    PROTEGEE;           // Someone I will receive alerts from (I'm their trusted contact)

    @JsonValue
    public String getValue() {
        return switch (this) {
            case TRUSTED_CONTACT -> "TRUSTED";
            case PROTEGEE -> "PROTEGEE";
        };
    }

    @JsonCreator
    public static ContactType fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return switch (value.toUpperCase()) {
                case "TRUSTED", "TRUSTED_CONTACT" -> TRUSTED_CONTACT;
                case "PROTEGEE" -> PROTEGEE;
                default -> throw new IllegalArgumentException("Invalid contact type: " + value);
            };
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid contact type value: " + value +
                ". Valid values are: TRUSTED, PROTEGEE", e);
        }
    }
}
