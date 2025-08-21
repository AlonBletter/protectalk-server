package com.protectalk.alerts.infra;

import com.google.firebase.messaging.*;
import com.protectalk.alerts.port.NotificationPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Adapter for sending notifications via Firebase Cloud Messaging (FCM).
 */
@Service
@RequiredArgsConstructor
public class FcmNotificationAdapter implements NotificationPort {
    private static final Logger logger = LoggerFactory.getLogger(FcmNotificationAdapter.class);
    private final FirebaseMessaging fcm;

    /**
     * Sends a composed message to one or more device tokens using FCM.
     * @param m the composed message
     * @return message ID or batch result
     * @throws FirebaseMessagingException if sending fails
     */
    @Override
    public String send(ComposedMessage m) throws FirebaseMessagingException {
        if (m == null || m.tokens() == null || m.tokens().isEmpty()) {
            logger.warn("No tokens provided for FCM notification");
            throw new IllegalArgumentException("No tokens provided");
        }
        if (m.title() == null || m.body() == null) {
            logger.warn("Notification title or body is missing");
            throw new IllegalArgumentException("Notification title or body is missing");
        }
        try {
            var notification = Notification.builder().setTitle(m.title()).setBody(m.body()).build();
            // send to single token
            if (m.tokens().size() == 1) {
                var message = Message.builder()
                        .setToken(m.tokens().get(0))
                        .setNotification(notification)
                        .putAllData(m.data())
                        .build();
                String messageId = fcm.send(message);
                logger.info("Sent FCM notification to single token: {}", m.tokens().get(0));
                return messageId;
            } else {
                var multicastMessage = MulticastMessage.builder()
                        .addAllTokens(m.tokens())
                        .putAllData(m.data())
                        .setNotification(notification)
                        .build();
                var resp = fcm.sendEachForMulticast(multicastMessage);
                logger.info("Sent FCM notification to {} tokens, {} succeeded", m.tokens().size(), resp.getSuccessCount());
                return "batch:" + resp.getSuccessCount();
            }
        } catch (FirebaseMessagingException e) {
            logger.error("Error sending FCM notification", e);
            throw e;
        }
    }
}
