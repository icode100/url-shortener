package com.service.url_shortener.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LinkMappingRepository extends JpaRepository<LinkMapping, Long> {

    Optional<LinkMapping> findByCode(String code);

    Optional<LinkMapping> findByDeduplicationKey(String deduplicationKey);
}
