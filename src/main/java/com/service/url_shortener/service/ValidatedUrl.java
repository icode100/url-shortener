package com.service.url_shortener.service;

public record ValidatedUrl(String originalUrl, String normalizedUrl) {
}
