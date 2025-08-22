package com.protectalk.alerts.infra;

import com.google.firebase.messaging.*;
import com.protectalk.alerts.port.NotificationPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FcmNotificationAdapter implements NotificationPort {
    private static final Logger log = LoggerFactory.getLogger(FcmNotificationAdapter.class);
    private final FirebaseMessaging fcm;

    @Override
    public NotificationResult send(ComposedMessage m) throws FirebaseMessagingException {
        if (m == null || m.tokens() == null || m.tokens().isEmpty())
            throw new IllegalArgumentException("No tokens provided");
        if (m.title() == null || m.body() == null)
            throw new IllegalArgumentException("Notification title or body is missing");

        var notification = Notification.builder()
                                       .setTitle(m.title())
                                       .setBody(m.body())
                                       .build();

        // single
        if (m.tokens().size() == 1) {
            var msg = Message.builder()
                             .setToken(m.tokens().get(0))
                             .setNotification(notification)
                             .putAllData(m.data() == null ? Map.of() : m.data())
                             .build();

            String messageId = fcm.send(msg);
            var d = new Delivery(m.tokens().get(0), true, messageId, null, null);
            return new NotificationResult(1, 1, 0, List.of(d), List.of());
        }

        // multicast
        var multi = MulticastMessage.builder()
                                    .addAllTokens(m.tokens())
                                    .setNotification(notification)
                                    .putAllData(m.data() == null ? Map.of() : m.data())
                                    .build();

        var resp = fcm.sendEachForMulticast(multi);

        var deliveries = new ArrayList<Delivery>(m.tokens().size());
        var invalidTokens = new ArrayList<String>();

        for (int i = 0; i < m.tokens().size(); i++) {
            var token = m.tokens().get(i);
            var r = resp.getResponses().get(i);

            if (r.isSuccessful()) {
                deliveries.add(new Delivery(token, true, r.getMessageId(), null, null));
            } else {
                var ex = r.getException(); // FirebaseMessagingException
                var code = (ex != null && ex.getMessagingErrorCode() != null)
                           ? ex.getMessagingErrorCode().name() : null;
                var msg  = (ex != null) ? ex.getMessage() : "Unknown FCM error";
                deliveries.add(new Delivery(token, false, null, code, msg));

                // tokens to prune
                if ("UNREGISTERED".equals(code) || "INVALID_ARGUMENT".equals(code)) {
                    invalidTokens.add(token);
                }
            }
        }

        log.info("FCM multicast: total={}, success={}, failure={}",
                 resp.getResponses().size(), resp.getSuccessCount(),
                 resp.getFailureCount());

        return new NotificationResult(
            resp.getResponses().size(),
            resp.getSuccessCount(),
            resp.getFailureCount(),
            deliveries,
            invalidTokens
        );
    }
}
