package com.protectalk.alerts.service;

import com.google.api.client.util.Value;
import com.protectalk.alerts.domain.RiskLevel;
import org.springframework.stereotype.Component;

@Component
public class ThresholdGate {
    @Value("${protectalk.threshold.score:0.75}") double minScore;

    public boolean allows(double score, RiskLevel level) {
        return level == RiskLevel.RED || score >= minScore;
    }
}
