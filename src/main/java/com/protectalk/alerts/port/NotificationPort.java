package com.protectalk.alerts.port;

import java.util.List;
import java.util.Map;

public interface NotificationPort {
    record ComposedMessage(
        String title,
        String body,
        Map<String, String> data,
        List<String> tokens
    ) {}

    String send(ComposedMessage m) throws Exception;
}
