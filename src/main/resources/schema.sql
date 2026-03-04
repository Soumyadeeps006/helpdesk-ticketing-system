-- =============================================================
-- IT Help Desk Ticketing System — Database Schema
-- =============================================================
-- Run this script ONCE to initialise the schema.
-- Hibernate hbm2ddl.auto=update will keep it in sync during dev.
--
-- Tables:
--   users        — employees and IT staff
--   tickets      — help-desk tickets with status lifecycle
--   comments     — threaded comments (self-referencing FK)
--   audit_log    — immutable record of every state change
-- =============================================================

CREATE DATABASE IF NOT EXISTS helpdesk_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE helpdesk_db;

-- ── Users ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(80)  NOT NULL,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,          -- BCrypt hash
    full_name     VARCHAR(150),
    role          VARCHAR(20)  NOT NULL,          -- EMPLOYEE | IT_STAFF | ADMIN
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_username (username),
    UNIQUE KEY uq_email    (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Tickets ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tickets (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'OPEN',     -- OPEN | IN_PROGRESS | CLOSED
    priority        VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM',   -- LOW | MEDIUM | HIGH | CRITICAL
    created_by_id   BIGINT       NOT NULL,
    assigned_to_id  BIGINT,                                    -- NULL until assigned
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    closed_at       DATETIME,

    PRIMARY KEY (id),
    CONSTRAINT fk_ticket_created_by  FOREIGN KEY (created_by_id)  REFERENCES users (id),
    CONSTRAINT fk_ticket_assigned_to FOREIGN KEY (assigned_to_id) REFERENCES users (id),

    INDEX idx_ticket_status   (status),
    INDEX idx_ticket_priority (priority),
    INDEX idx_ticket_created  (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Comments (self-referencing) ────────────────────────────────
--
-- parent_id NULL  → top-level comment on a ticket
-- parent_id = N   → reply to comment N (forms a tree)
--
CREATE TABLE IF NOT EXISTS comments (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    body        TEXT     NOT NULL,
    ticket_id   BIGINT   NOT NULL,
    author_id   BIGINT   NOT NULL,
    parent_id   BIGINT,                           -- self-reference: NULL = root comment
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_comment_ticket FOREIGN KEY (ticket_id) REFERENCES tickets  (id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_author FOREIGN KEY (author_id) REFERENCES users    (id),
    CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES comments (id) ON DELETE CASCADE,

    INDEX idx_comment_ticket (ticket_id),
    INDEX idx_comment_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Audit Log ──────────────────────────────────────────────────
--
-- Every mutation to a ticket is captured here:
--   action    : STATUS_CHANGE | PRIORITY_CHANGE | COMMENT_ADDED |
--               TICKET_CREATED | ASSIGNMENT_CHANGE
--   old_value : previous state (NULL for TICKET_CREATED)
--   new_value : new state after the change
--
CREATE TABLE IF NOT EXISTS audit_log (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    ticket_id  BIGINT      NOT NULL,
    actor_id   BIGINT      NOT NULL,
    action     VARCHAR(50) NOT NULL,
    old_value  VARCHAR(100),
    new_value  VARCHAR(100) NOT NULL,
    changed_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_audit_ticket FOREIGN KEY (ticket_id) REFERENCES tickets (id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_actor  FOREIGN KEY (actor_id)  REFERENCES users   (id),

    INDEX idx_audit_ticket     (ticket_id),
    INDEX idx_audit_changed_at (changed_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Seed data (dev only) ───────────────────────────────────────
-- Password for all seed users: "password123" (BCrypt hash below)
INSERT IGNORE INTO users (username, email, password_hash, full_name, role) VALUES
  ('admin',       'admin@helpdesk.local',    '$2a$10$xJwL5v5Jz4a.YbFT2Z.rLe7V0Lk8FmK9dR1N3oQ6pS2uY4tH8wA6', 'System Admin',  'ADMIN'),
  ('it_alice',    'alice@helpdesk.local',    '$2a$10$xJwL5v5Jz4a.YbFT2Z.rLe7V0Lk8FmK9dR1N3oQ6pS2uY4tH8wA6', 'Alice Smith',   'IT_STAFF'),
  ('emp_bob',     'bob@helpdesk.local',      '$2a$10$xJwL5v5Jz4a.YbFT2Z.rLe7V0Lk8FmK9dR1N3oQ6pS2uY4tH8wA6', 'Bob Johnson',   'EMPLOYEE');
