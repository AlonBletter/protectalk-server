package com.protectalk.alerts.domain;

public enum Platform {
    ANDROID,
    IOS,
    WEB,
    DESKTOP;

    public String getValue() {
        return name().toLowerCase();
    }
}
