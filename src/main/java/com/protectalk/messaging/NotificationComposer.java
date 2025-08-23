package com.protectalk.messaging;

import com.protectalk.alert.model.AlertRecordEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.protectalk.alert.model.RiskLevel.YELLOW;

@Component
public class NotificationComposer {
    public OutboundMessage compose(AlertRecordEntity saved, List<String> tokens) {
        String title = switch (saved.getRiskLevel()) {
            case RED -> "⚠️ High Scam Risk";
            case YELLOW -> "⚠ Potential Scam";
            default -> "Info";
        };
        // TODO better message body with the victims details - name + relationship
        String body = "Suspicious call from " + saved.getCallerNumber();
        Map<String, String> data = Map.of(
                "callId", saved.getId(),
                "riskLevel", saved.getRiskLevel().name(),
                "phoneNumber", saved.getCallerNumber(),
                "modelAnalysis", saved.getModelAnalysis()
        );
        return new OutboundMessage(title, body, data, tokens);
    }
}
