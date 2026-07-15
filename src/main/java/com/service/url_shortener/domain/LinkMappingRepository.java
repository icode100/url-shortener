package com.service.url_shortener.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface LinkMappingRepository extends JpaRepository<LinkMapping, Long> {

    Optional<LinkMapping> findByCode(String code);

    Optional<LinkMapping> findByDeduplicationKey(String deduplicationKey);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update LinkMapping mapping
               set mapping.redirectCount = mapping.redirectCount + 1,
                   mapping.lastAccessedAt = :accessedAt
             where mapping.id = :id
            """)
    int recordRedirect(@Param("id") long id, @Param("accessedAt") Instant accessedAt);
}
