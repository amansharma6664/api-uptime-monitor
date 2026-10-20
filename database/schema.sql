-- OPTIONAL: Spring Boot ye tables apne aap bana deta hai (ddl-auto=update).
-- Agar tum MySQL me manually setup karna chahte ho to ye script chala do.
-- Command:  mysql -u root -p < database/schema.sql

CREATE DATABASE IF NOT EXISTS uptime_monitor CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE uptime_monitor;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL,
  password VARCHAR(255) NOT NULL,
  role VARCHAR(255) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS monitors (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  url VARCHAR(2048) NOT NULL,
  http_method VARCHAR(10) NOT NULL,
  interval_seconds INT NOT NULL,
  timeout_seconds INT NOT NULL,
  expected_status INT NOT NULL,
  active BIT NOT NULL,
  webhook_url VARCHAR(1024),
  alert_email VARCHAR(255),
  status VARCHAR(20) NOT NULL,
  consecutive_failures INT NOT NULL,
  last_checked_at DATETIME(6),
  last_response_time_ms BIGINT,
  next_check_at DATETIME(6),
  created_at DATETIME(6) NOT NULL,
  owner_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_monitors_owner FOREIGN KEY (owner_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS check_results (
  id BIGINT NOT NULL AUTO_INCREMENT,
  monitor_id BIGINT NOT NULL,
  status VARCHAR(10) NOT NULL,
  status_code INT,
  response_time_ms BIGINT NOT NULL,
  error_message VARCHAR(500),
  checked_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_check_monitor_time (monitor_id, checked_at),
  CONSTRAINT fk_check_monitor FOREIGN KEY (monitor_id) REFERENCES monitors (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS incidents (
  id BIGINT NOT NULL AUTO_INCREMENT,
  monitor_id BIGINT NOT NULL,
  started_at DATETIME(6) NOT NULL,
  resolved_at DATETIME(6),
  reason VARCHAR(500),
  PRIMARY KEY (id),
  CONSTRAINT fk_incident_monitor FOREIGN KEY (monitor_id) REFERENCES monitors (id)
) ENGINE=InnoDB;
