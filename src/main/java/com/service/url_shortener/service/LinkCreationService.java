package com.service.url_shortener.service;

import com.service.url_shortener.domain.LinkMapping;
import com.service.url_shortener.domain.LinkMappingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
class LinkCreationService {

    private static final String GENERATED_CODE_PREFIX = "_";

    private final LinkMappingRepository repository;
    private final LinkSequenceAllocator sequenceAllocator;
    private final Clock clock = Clock.systemUTC();

    LinkCreationService(LinkMappingRepository repository, LinkSequenceAllocator sequenceAllocator) {
        this.repository = repository;
        this.sequenceAllocator = sequenceAllocator;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LinkMapping createGenerated(String originalUrl, String normalizedUrl) {
        long id = sequenceAllocator.nextId();
        String code = GENERATED_CODE_PREFIX + Base62Encoder.encode(id);
        LinkMapping mapping = LinkMapping.generated(id, code, originalUrl, normalizedUrl, Instant.now(clock));
        return repository.saveAndFlush(mapping);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LinkMapping createCustom(String alias, String originalUrl, String normalizedUrl) {
        long id = sequenceAllocator.nextId();
        LinkMapping mapping = LinkMapping.custom(id, alias, originalUrl, normalizedUrl, Instant.now(clock));
        return repository.saveAndFlush(mapping);
    }
}
