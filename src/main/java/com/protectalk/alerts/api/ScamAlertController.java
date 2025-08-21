package com.protectalk.alerts.api;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.protectalk.alerts.dto.ScamAlertRequestDto;
import com.protectalk.alerts.dto.ScamAlertResponseDto;
import com.protectalk.alerts.service.AlertOrchestratorService;
import com.protectalk.security.model.FirebasePrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class ScamAlertController {
    private final AlertOrchestratorService orchestrator;

    @PostMapping
    public ResponseEntity<ScamAlertResponseDto> create(@AuthenticationPrincipal FirebasePrincipal me,
                                                       @RequestBody @Valid ScamAlertRequestDto req)
        throws FirebaseMessagingException {
        return ResponseEntity.ok(orchestrator.handle(me.uid(), req));
    }
}
