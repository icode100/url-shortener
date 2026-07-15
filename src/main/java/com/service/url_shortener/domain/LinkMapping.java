package com.service.url_shortener.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "link_mappings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_link_mappings_code", columnNames = "code"),
                @UniqueConstraint(name = "uk_link_mappings_deduplication_key", columnNames = "deduplication_key")
        },
        indexes = @Index(name = "idx_link_mappings_created_at", columnList = "created_at")
)
public class LinkMapping implements Persistable<Long> {

    @Id
    private Long id;

    @Column(nullable = false, length = 32, updatable = false)
    private String code;

    @Column(name = "original_url", nullable = false, length = 2048, updatable = false)
    private String originalUrl;

    @Column(name = "normalized_url", nullable = false, length = 2048, updatable = false)
    private String normalizedUrl;

    @Column(name = "deduplication_key", length = 2048, updatable = false)
    private String deduplicationKey;

    @Column(name = "custom_alias", nullable = false, updatable = false)
    private boolean customAlias;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "redirect_count", nullable = false)
    private long redirectCount;

    @Column(name = "last_accessed_at")
    private Instant lastAccessedAt;

    @Transient
    private boolean newEntity = true;

    protected LinkMapping() {
        // Required by JPA.
    }

    private LinkMapping(
            long id,
            String code,
            String originalUrl,
            String normalizedUrl,
            String deduplicationKey,
            boolean customAlias,
            Instant createdAt
    ) {
        this.id = id;
        this.code = Objects.requireNonNull(code);
        this.originalUrl = Objects.requireNonNull(originalUrl);
        this.normalizedUrl = Objects.requireNonNull(normalizedUrl);
        this.deduplicationKey = deduplicationKey;
        this.customAlias = customAlias;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static LinkMapping generated(
            long id,
            String code,
            String originalUrl,
            String normalizedUrl,
            Instant createdAt
    ) {
        return new LinkMapping(id, code, originalUrl, normalizedUrl, normalizedUrl, false, createdAt);
    }

    public static LinkMapping custom(
            long id,
            String alias,
            String originalUrl,
            String normalizedUrl,
            Instant createdAt
    ) {
        return new LinkMapping(id, alias, originalUrl, normalizedUrl, null, true, createdAt);
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        newEntity = false;
    }

    public String getCode() {
        return code;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getNormalizedUrl() {
        return normalizedUrl;
    }

    public String getDeduplicationKey() {
        return deduplicationKey;
    }

    public boolean isCustomAlias() {
        return customAlias;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public long getRedirectCount() {
        return redirectCount;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }
}
