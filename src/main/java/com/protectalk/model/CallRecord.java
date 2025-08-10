package com.protectalk.model;

public class CallRecord {
    private String userId;
    private String script;

    // Default constructor - required for Spring
    public CallRecord() {}

    public CallRecord(String userId, String script) {
        this.userId = userId;
        this.script = script;
    }

    // Getters and setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    @Override
    public String toString() {
        return "CallRecord{userId='" + userId + "', script='" + script + "'}";
    }
}