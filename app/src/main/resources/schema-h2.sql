-- H2 Database Schema
CREATE TABLE IF NOT EXISTS urls (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS url_checks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    url_id BIGINT,
    status_code INT,
    title VARCHAR(255),
    h1 VARCHAR(255),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (url_id) REFERENCES urls(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_url_checks_url_id ON url_checks(url_id);
CREATE INDEX IF NOT EXISTS idx_url_checks_created_at ON url_checks(created_at);