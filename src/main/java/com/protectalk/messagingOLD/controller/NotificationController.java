package com.protectalk.messagingOLD.controller;

import com.protectalk.messagingOLD.dto.NotificationRequestDto;
import com.protectalk.messagingOLD.dto.NotificationResponseDto;
import com.protectalk.messagingOLD.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<NotificationResponseDto> send(@RequestBody NotificationRequestDto request) {
        return ResponseEntity.ok(notificationService.sendNotification(request));
    }
}
