package com.protectalk.messaging;

import com.google.firebase.messaging.FirebaseMessagingException;

public interface NotificationGateway {
    NotificationResult send(OutboundMessage m) throws FirebaseMessagingException;
}
