package com.service.url_shortener.api;

import java.time.Instant;

public record LinkAnalyticsResponse(
        String code,
        String originalUrl,
        boolean customAlias,
        Instant createdAt,
        long redirectCount,
        Instant lastAccessedAt
) {
}
