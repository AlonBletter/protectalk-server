package com.protectalk.device.model;

public enum Platform {
    ANDROID,
    IOS,
    WEB,
    DESKTOP;

    public String getValue() {
        return name().toLowerCase();
    }
}
