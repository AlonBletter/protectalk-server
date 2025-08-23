package com.protectalk.messaging;

import java.util.List;
import java.util.Map;

public record OutboundMessage(
    String title,
    String body,
    Map<String, String> data,
    List<String> tokens
) {}
