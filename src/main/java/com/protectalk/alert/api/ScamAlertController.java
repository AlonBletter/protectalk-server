package com.protectalk.alert.api;

import com.protectalk.alert.dto.ScamAlertRequestDto;
import com.protectalk.alert.dto.ScamAlertResponseDto;
import com.protectalk.alert.service.ScamAlertService;
import com.protectalk.security.model.FirebasePrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Slf4j
public class ScamAlertController {
    private final ScamAlertService orchestrator;

    @PostMapping("/report")
    public ResponseEntity<ScamAlertResponseDto> report(@AuthenticationPrincipal FirebasePrincipal me,
                                                       @RequestBody @Valid ScamAlertRequestDto req) throws Exception {
        log.info("Scam alert received from UID: {} for caller: {} with risk: {} score: {}",
                me.uid(), req.callerNumber(), req.riskLevel(), req.modelScore());

        try {
            ScamAlertResponseDto response = orchestrator.handle(me.uid(), req);
            log.info("Scam alert processed - UID: {} eventId: {} notified: {} reason: {}",
                    me.uid(), req.eventId(), response.notified(), response.reason());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to process scam alert for UID: {} eventId: {}", me.uid(), req.eventId(), e);
            throw e;
        }
    }
}
