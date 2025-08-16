package com.protectalk.messaging.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.protectalk.messaging.dto.NotificationRequestDto;
import com.protectalk.messaging.dto.NotificationResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class FcmNotificationService implements NotificationService {

    private final FirebaseMessaging firebaseMessaging;

    @Override
    public NotificationResponseDto sendNotification(NotificationRequestDto request) {
        try {
            Message message = Message.builder()
                    .setToken(request.targetToken())
                    .setNotification(
                            Notification.builder()
                                    .setTitle(request.title())
                                    .setBody(request.body())
                                    .build()
                    )
                    .putAllData(Map.of(
                            "callId", request.data().callId(),
                            "riskLevel", request.data().riskLevel(),
                            "phoneNumber", request.data().phoneNumber()
                    ))
                    .build();

            String messageId = firebaseMessaging.send(message);
            // Save to DB
            return new NotificationResponseDto(true, messageId, null);
        } catch (FirebaseMessagingException e) {
            return new NotificationResponseDto(false, null, e.getMessage());
        }
    }
}
