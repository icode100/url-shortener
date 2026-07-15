package com.service.url_shortener.api;

import com.service.url_shortener.domain.LinkMapping;
import com.service.url_shortener.service.LinkResolutionService;
import com.service.url_shortener.service.LinkShorteningService;
import com.service.url_shortener.service.ShortenResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class UrlShortenerController {

    private final LinkShorteningService shorteningService;
    private final LinkResolutionService resolutionService;
    private final String baseUrl;

    public UrlShortenerController(
            LinkShorteningService shorteningService,
            LinkResolutionService resolutionService,
            @Value("${shortener.base-url:http://localhost:8080}") String baseUrl
    ) {
        this.shorteningService = shorteningService;
        this.resolutionService = resolutionService;
        this.baseUrl = stripTrailingSlashes(baseUrl);
    }

    @PostMapping(
            path = "/shorten",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        ShortenResult result = shorteningService.shorten(request.url(), request.customAlias());
        LinkMapping mapping = result.mapping();
        String shortUrl = baseUrl + "/" + mapping.getCode();
        ShortenResponse response = new ShortenResponse(
                mapping.getCode(),
                shortUrl,
                mapping.getOriginalUrl(),
                mapping.isCustomAlias(),
                mapping.getCreatedAt()
        );

        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status)
                .location(URI.create(shortUrl))
                .body(response);
    }

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        LinkMapping mapping = resolutionService.resolveAndRecord(code);
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .location(URI.create(mapping.getOriginalUrl()))
                .build();
    }

    @GetMapping(path = "/api/links/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
    public LinkAnalyticsResponse analytics(@PathVariable String code) {
        LinkMapping mapping = resolutionService.getAnalytics(code);
        return new LinkAnalyticsResponse(
                mapping.getCode(),
                mapping.getOriginalUrl(),
                mapping.isCustomAlias(),
                mapping.getCreatedAt(),
                mapping.getRedirectCount(),
                mapping.getLastAccessedAt()
        );
    }

    private static String stripTrailingSlashes(String value) {
        String result = value.strip();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        if (result.isBlank()) {
            throw new IllegalArgumentException("shortener.base-url must not be blank");
        }
        return result;
    }
}
