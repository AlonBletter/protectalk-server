package com.protectalk.alerts.port;

import com.google.firebase.messaging.FirebaseMessagingException;

import java.util.List;
import java.util.Map;

public interface NotificationPort {
    String send(ComposedMessage message) throws FirebaseMessagingException; // returns FCM message id
    record ComposedMessage(List<String> tokens, String title, String body, Map<String,String> data) {}
}
