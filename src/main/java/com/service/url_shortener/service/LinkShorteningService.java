package com.service.url_shortener.service;

import com.service.url_shortener.domain.LinkMapping;
import com.service.url_shortener.domain.LinkMappingRepository;
import com.service.url_shortener.exception.AliasConflictException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class LinkShorteningService {

    private final LinkMappingRepository repository;
    private final LinkCreationService creationService;
    private final UrlNormalizer urlNormalizer;
    private final AliasPolicy aliasPolicy;

    public LinkShorteningService(
            LinkMappingRepository repository,
            LinkCreationService creationService,
            UrlNormalizer urlNormalizer,
            AliasPolicy aliasPolicy
    ) {
        this.repository = repository;
        this.creationService = creationService;
        this.urlNormalizer = urlNormalizer;
        this.aliasPolicy = aliasPolicy;
    }

    public ShortenResult shorten(String rawUrl, String requestedAlias) {
        ValidatedUrl url = urlNormalizer.validate(rawUrl);
        String alias = aliasPolicy.validate(requestedAlias);
        return alias == null
                ? shortenGenerated(url)
                : shortenCustom(url, alias);
    }

    private ShortenResult shortenGenerated(ValidatedUrl url) {
        return repository.findByDeduplicationKey(url.normalizedUrl())
                .map(mapping -> new ShortenResult(mapping, false))
                .orElseGet(() -> createGenerated(url));
    }

    private ShortenResult createGenerated(ValidatedUrl url) {
        try {
            LinkMapping mapping = creationService.createGenerated(url.originalUrl(), url.normalizedUrl());
            return new ShortenResult(mapping, true);
        } catch (DataIntegrityViolationException concurrentInsert) {
            LinkMapping winner = repository.findByDeduplicationKey(url.normalizedUrl())
                    .orElseThrow(() -> concurrentInsert);
            return new ShortenResult(winner, false);
        }
    }

    private ShortenResult shortenCustom(ValidatedUrl url, String alias) {
        return repository.findByCode(alias)
                .map(existing -> resolveExistingAlias(existing, url.normalizedUrl(), alias))
                .orElseGet(() -> createCustom(url, alias));
    }

    private ShortenResult createCustom(ValidatedUrl url, String alias) {
        try {
            LinkMapping mapping = creationService.createCustom(alias, url.originalUrl(), url.normalizedUrl());
            return new ShortenResult(mapping, true);
        } catch (DataIntegrityViolationException concurrentInsert) {
            LinkMapping winner = repository.findByCode(alias)
                    .orElseThrow(() -> concurrentInsert);
            return resolveExistingAlias(winner, url.normalizedUrl(), alias);
        }
    }

    private ShortenResult resolveExistingAlias(LinkMapping existing, String normalizedUrl, String alias) {
        if (existing.getNormalizedUrl().equals(normalizedUrl)) {
            return new ShortenResult(existing, false);
        }
        throw new AliasConflictException(alias);
    }
}
