package com.protectalk.device.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Platform {
    ANDROID,
    IOS,
    WEB,
    DESKTOP;

    @JsonValue
    public String getValue() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static Platform fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Platform.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid platform value: " + value + 
                ". Valid values are: android, ios, web, desktop", e);
        }
    }
}
