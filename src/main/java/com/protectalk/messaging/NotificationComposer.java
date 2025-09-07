package com.protectalk.messaging;

import com.protectalk.alert.model.AlertRecordEntity;
import com.protectalk.usermanagment.repo.UserRepository;
import com.protectalk.usermanagment.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationComposer {

    private final UserRepository userRepository;

    public OutboundMessage compose(AlertRecordEntity saved, List<String> tokens) {
        String title = switch (saved.getRiskLevel()) {
            case RED -> "🚨 URGENT: Scam Alert";
            case YELLOW -> "⚠️ Scam Warning";
            default -> "📞 Call Alert";
        };

        // Get victim details for detailed message
        String victimName = userRepository.findByFirebaseUid(saved.getUserId())
                .map(UserEntity::getName)
                .orElse("Your contact");

        // Create detailed message body with victim info, analysis, and score
        String body = createDetailedScamMessage(victimName, saved);

        Map<String, String> data = Map.of(
                "callId", saved.getId(),
                "riskLevel", saved.getRiskLevel().name(),
                "phoneNumber", saved.getCallerNumber(),
                "modelAnalysis", saved.getModelAnalysis(),
                "victimName", victimName,
                "riskScore", String.valueOf(Math.round(saved.getModelScore() * 100))
        );
        return new OutboundMessage(title, body, data, tokens);
    }

    private String createDetailedScamMessage(String victimName, AlertRecordEntity alert) {
        int riskPercentage = (int) Math.round(alert.getModelScore() * 100);

        StringBuilder message = new StringBuilder();
        message.append(String.format("🚨 %s may be getting scammed!\n\n", victimName));
        message.append(String.format("📞 Caller: %s\n", alert.getCallerNumber()));
        message.append(String.format("🎯 Risk Score: %d%%\n", riskPercentage));

        if (alert.getModelAnalysis() != null && !alert.getModelAnalysis().trim().isEmpty()) {
            message.append(String.format("🔍 Analysis: %s\n", alert.getModelAnalysis()));
        }

        if (alert.getDurationInSeconds() > 0) {
            int minutes = alert.getDurationInSeconds() / 60;
            int seconds = alert.getDurationInSeconds() % 60;
            if (minutes > 0) {
                message.append(String.format("⏱️ Call Duration: %dm %ds\n", minutes, seconds));
            } else {
                message.append(String.format("⏱️ Call Duration: %ds\n", seconds));
            }
        }

        message.append("\n💡 Consider reaching out to check on them immediately.");

        return message.toString();
    }
}
