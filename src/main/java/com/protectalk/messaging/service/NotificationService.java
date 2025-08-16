package com.protectalk.messaging.service;

import com.protectalk.messaging.dto.NotificationRequestDto;
import com.protectalk.messaging.dto.NotificationResponseDto;

public interface NotificationService {
    NotificationResponseDto sendNotification(NotificationRequestDto request);
}
