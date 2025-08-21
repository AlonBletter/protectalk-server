package com.protectalk.messagingOLD.service;

import com.protectalk.messagingOLD.dto.NotificationRequestDto;
import com.protectalk.messagingOLD.dto.NotificationResponseDto;

public interface NotificationService {
    NotificationResponseDto sendNotification(NotificationRequestDto request);
}
