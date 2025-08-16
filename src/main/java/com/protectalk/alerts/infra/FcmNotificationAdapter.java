package com.protectalk.alerts.infra;

import com.google.firebase.messaging.*;
import com.protectalk.alerts.port.NotificationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FcmNotificationAdapter implements NotificationPort {
    private final FirebaseMessaging fcm;

    @Override
    public String send(ComposedMessage m) throws FirebaseMessagingException {
        var message = Message.builder()
                .putAllData(m.data())
                .setNotification(Notification.builder().setTitle(m.title()).setBody(m.body()).build())
                .build();

        // send to multiple tokens
        if (m.tokens().size() == 1) {
            return fcm.send(Message.builder()
                    .setToken(m.tokens().get(0))
                    .setNotification(Notification.builder().setTitle(m.title()).setBody(m.body()).build())
                    .putAllData(m.data())
                    .build());
        } else {
            var resp = fcm.sendEachForMulticast(
                    MulticastMessage.builder()
                            .addAllTokens(m.tokens())
                            .putAllData(m.data())
                            .setNotification(Notification.builder().setTitle(m.title()).setBody(m.body()).build())
                            .build()
            );
            // you can store per-token response if you need; return a synthetic id or first success
            return "batch:" + resp.getSuccessCount();
        }
    }
}
