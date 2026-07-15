package com.service.url_shortener.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class LinkSequenceAllocator {

    private final JdbcTemplate jdbcTemplate;
    private final String sequenceQuery;

    LinkSequenceAllocator(
            JdbcTemplate jdbcTemplate,
            @Value("${shortener.sequence-query}") String sequenceQuery
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.sequenceQuery = sequenceQuery;
    }

    long nextId() {
        Long id = jdbcTemplate.queryForObject(sequenceQuery, Long.class);
        if (id == null || id <= 0) {
            throw new IllegalStateException("Database sequence did not return a positive ID");
        }
        return id;
    }
}
