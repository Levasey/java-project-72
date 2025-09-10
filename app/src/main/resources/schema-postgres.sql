-- PostgreSQL Database Schema
CREATE TABLE IF NOT EXISTS urls (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS url_checks (
    id BIGSERIAL PRIMARY KEY,
    url_id BIGINT REFERENCES urls(id) ON DELETE CASCADE,
    status_code INTEGER,
    title VARCHAR(255),
    h1 VARCHAR(255),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_url_checks_url_id ON url_checks(url_id);
CREATE INDEX IF NOT EXISTS idx_url_checks_created_at ON url_checks(created_at);
CREATE INDEX IF NOT EXISTS idx_urls_name ON urls(name);