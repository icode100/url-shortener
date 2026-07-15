CREATE SEQUENCE link_mapping_sequence
    START WITH 1000000000
    INCREMENT BY 1
    NO CYCLE;

CREATE TABLE link_mappings (
    id BIGINT PRIMARY KEY,
    code VARCHAR(32) NOT NULL,
    original_url VARCHAR(2048) NOT NULL,
    normalized_url VARCHAR(2048) NOT NULL,
    deduplication_key VARCHAR(2048),
    custom_alias BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    redirect_count BIGINT NOT NULL DEFAULT 0,
    last_accessed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_link_mappings_code UNIQUE (code),
    CONSTRAINT uk_link_mappings_deduplication_key UNIQUE (deduplication_key)
);

CREATE INDEX idx_link_mappings_created_at ON link_mappings (created_at);
