package com.protectalk.alerts.dto;

import com.protectalk.alerts.domain.RiskLevel;

import java.time.Instant;

public record ScamAlertRequestDto(String eventId,          // client-generated UUID for idempotency
                                  String callerNumber,     // E.164
                                  double modelScore,            // model modelScore used by client
                                  RiskLevel riskLevel,     // client’s classification (expect RED or >= threshold)
                                  String transcript,       // optional (or hash/summary)
                                  String modelAnalysis,    // optional model analysis summary
                                  Instant occurredAt,       // when the call happened
                                  int durationInSeconds) {
}