-- V3: back-office accounts. Passwords are stored ONLY as one-way hashes (bcrypt, {bcrypt} prefix).
CREATE TABLE admin_user (
    id            UUID PRIMARY KEY,
    email         VARCHAR(160) NOT NULL,
    display_name  VARCHAR(120) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'ADMIN' CHECK (role IN ('ADMIN')),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Emails are compared case-insensitively.
CREATE UNIQUE INDEX uq_admin_user_email ON admin_user (lower(email));
