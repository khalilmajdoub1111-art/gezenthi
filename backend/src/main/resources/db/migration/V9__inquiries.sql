-- V9: quote requests ("demandes de devis") for offers without online booking:
-- organised trips, Omra, visa, flights, CTN/TRE, partner hotels without a live rate.
-- Contains PERSONAL DATA: admin-only access, never logged.

CREATE SEQUENCE inquiry_ref_seq;

CREATE TABLE inquiry (
    id                  UUID PRIMARY KEY,
    reference           VARCHAR(20)  NOT NULL UNIQUE,
    status              VARCHAR(20)  NOT NULL CHECK (status IN ('NEW', 'IN_PROGRESS', 'DONE', 'CANCELLED')),
    type                VARCHAR(10)  NOT NULL CHECK (type IN ('HOTEL', 'TRIP', 'OMRA', 'VISA', 'FLIGHT', 'FERRY', 'OTHER')),
    subject             VARCHAR(200) NOT NULL,
    offer_ref           VARCHAR(80),
    date_from           DATE,
    date_to             DATE,
    adults              INTEGER      NOT NULL CHECK (adults BETWEEN 1 AND 20),
    children            INTEGER      NOT NULL DEFAULT 0 CHECK (children BETWEEN 0 AND 10),
    message             VARCHAR(1500),
    customer_civility   VARCHAR(5)   NOT NULL,
    customer_first_name VARCHAR(80)  NOT NULL,
    customer_last_name  VARCHAR(80)  NOT NULL,
    customer_email      VARCHAR(160) NOT NULL,
    customer_phone      VARCHAR(30)  NOT NULL,
    whatsapp_opt_in     BOOLEAN      NOT NULL DEFAULT FALSE,
    consent_at          TIMESTAMPTZ  NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_inquiry_status ON inquiry (status, created_at DESC);

CREATE TABLE inquiry_event (
    id         UUID PRIMARY KEY,
    inquiry_id UUID         NOT NULL REFERENCES inquiry (id) ON DELETE CASCADE,
    at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actor      VARCHAR(160) NOT NULL,
    action     VARCHAR(40)  NOT NULL,
    details    VARCHAR(1000)
);
CREATE INDEX idx_inquiry_event ON inquiry_event (inquiry_id, at);

-- Notifications can now be about a booking OR a quote request.
ALTER TABLE notification_log ADD COLUMN inquiry_id UUID REFERENCES inquiry (id) ON DELETE CASCADE;
