package com.service.url_shortener.service;

import com.service.url_shortener.domain.LinkMapping;
import com.service.url_shortener.domain.LinkMappingRepository;
import com.service.url_shortener.exception.LinkNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class LinkResolutionService {

    private final LinkMappingRepository repository;
    private final Clock clock = Clock.systemUTC();

    public LinkResolutionService(LinkMappingRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public LinkMapping resolveAndRecord(String code) {
        LinkMapping mapping = find(code);
        int updated = repository.recordRedirect(mapping.getId(), Instant.now(clock));
        if (updated != 1) {
            throw new LinkNotFoundException(code);
        }
        return mapping;
    }

    @Transactional(readOnly = true)
    public LinkMapping getAnalytics(String code) {
        return find(code);
    }

    private LinkMapping find(String code) {
        return repository.findByCode(code)
                .orElseThrow(() -> new LinkNotFoundException(code));
    }
}
