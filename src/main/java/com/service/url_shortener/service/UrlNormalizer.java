package com.service.url_shortener.service;

import com.service.url_shortener.exception.InvalidUrlException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

@Component
public class UrlNormalizer {

    public static final int MAX_URL_LENGTH = 2048;

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    public ValidatedUrl validate(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new InvalidUrlException("URL is required");
        }

        String candidate = rawUrl.strip();
        if (candidate.length() > MAX_URL_LENGTH) {
            throw new InvalidUrlException("URL must not exceed " + MAX_URL_LENGTH + " characters");
        }

        URI parsed;
        try {
            parsed = new URI(candidate).normalize();
        } catch (URISyntaxException exception) {
            throw new InvalidUrlException("URL is malformed");
        }

        String scheme = parsed.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) {
            throw new InvalidUrlException("URL must use the http or https scheme");
        }
        scheme = scheme.toLowerCase(Locale.ROOT);

        if (parsed.getUserInfo() != null) {
            throw new InvalidUrlException("URLs containing user information are not accepted");
        }

        String host = parsed.getHost();
        if (host == null || host.isBlank()) {
            throw new InvalidUrlException("URL must contain a valid host");
        }
        host = host.toLowerCase(Locale.ROOT);

        int port = parsed.getPort();
        if (port == 0 || port > 65_535) {
            throw new InvalidUrlException("URL contains an invalid port");
        }
        if ((scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443)) {
            port = -1;
        }

        String normalized = buildNormalizedUrl(parsed, scheme, host, port);
        if (normalized.length() > MAX_URL_LENGTH) {
            throw new InvalidUrlException("URL must not exceed " + MAX_URL_LENGTH + " characters");
        }
        return new ValidatedUrl(candidate, normalized);
    }

    public String normalize(String rawUrl) {
        return validate(rawUrl).normalizedUrl();
    }

    private String buildNormalizedUrl(URI parsed, String scheme, String host, int port) {
        StringBuilder normalized = new StringBuilder(scheme).append("://");
        if (host.indexOf(':') >= 0 && !host.startsWith("[")) {
            normalized.append('[').append(host).append(']');
        } else {
            normalized.append(host);
        }
        if (port >= 0) {
            normalized.append(':').append(port);
        }

        String path = parsed.getRawPath();
        normalized.append(path == null || path.isEmpty() ? "/" : path);
        if (parsed.getRawQuery() != null) {
            normalized.append('?').append(parsed.getRawQuery());
        }
        if (parsed.getRawFragment() != null) {
            normalized.append('#').append(parsed.getRawFragment());
        }
        return normalized.toString();
    }
}
