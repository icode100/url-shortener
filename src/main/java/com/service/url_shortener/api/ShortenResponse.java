package com.service.url_shortener.api;

import java.time.Instant;

public record ShortenResponse(
        String code,
        String shortUrl,
        String originalUrl,
        boolean customAlias,
        Instant createdAt
) {
}
