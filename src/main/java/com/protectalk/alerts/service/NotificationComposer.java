package com.protectalk.alerts.service;

import com.protectalk.alerts.port.CallRecordPort;
import com.protectalk.alerts.port.NotificationPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class NotificationComposer {
    public NotificationPort.ComposedMessage compose(CallRecordPort.SavedCallRecord saved, List<String> tokens) {
        String title = switch (saved.risk()) {
            case RED -> "⚠️ High Scam Risk";
            case YELLOW -> "⚠ Potential Scam";
            default -> "Info";
        };
        String body = "Suspicious call from " + saved.callerNumber();
        Map<String,String> data = Map.of(
                "callId", saved.id(),
                "riskLevel", saved.risk().name(),
                "phoneNumber", saved.callerNumber()
        );
        return new NotificationPort.ComposedMessage(tokens, title, body, data);
    }
}
