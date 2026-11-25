-- V6: booking requests made on the website.
-- Contains PERSONAL DATA (customer + passengers): admin-only access, never logged,
-- no passport numbers collected. Price/hotel/room are copied from the server-side
-- offer at request time (never trusted from the browser).

CREATE SEQUENCE booking_ref_seq;

CREATE TABLE booking_request (
    id                    UUID PRIMARY KEY,
    reference             VARCHAR(20)  NOT NULL UNIQUE,
    status                VARCHAR(20)  NOT NULL
        CHECK (status IN ('NEW', 'IN_PROGRESS', 'PRICE_CHECKED', 'CONFIRMED', 'CANCELLED', 'REJECTED')),
    origin                VARCHAR(12)  NOT NULL CHECK (origin IN ('agence', 'partenaire')),

    hotel_ref             VARCHAR(60),
    hotel_name            VARCHAR(200) NOT NULL,
    city                  VARCHAR(120),
    stars                 INTEGER,
    supplier_code         VARCHAR(40),
    supplier_hotel_id     VARCHAR(40),
    supplier_city_id      VARCHAR(40),

    check_in              DATE         NOT NULL,
    check_out             DATE         NOT NULL,
    adults                INTEGER      NOT NULL CHECK (adults BETWEEN 1 AND 6),
    child_ages            INTEGER[]    NOT NULL DEFAULT '{}',
    board_id              VARCHAR(40),
    board_name            VARCHAR(120),
    room_id               VARCHAR(40),
    room_name             VARCHAR(200),
    total_price           NUMERIC(12, 3) NOT NULL CHECK (total_price >= 0),
    currency              VARCHAR(3)   NOT NULL,
    refundable            BOOLEAN      NOT NULL,
    cancellation_deadline VARCHAR(40),

    customer_civility     VARCHAR(5)   NOT NULL,
    customer_first_name   VARCHAR(80)  NOT NULL,
    customer_last_name    VARCHAR(80)  NOT NULL,
    customer_email        VARCHAR(160) NOT NULL,
    customer_phone        VARCHAR(30)  NOT NULL,
    comment               VARCHAR(1000),
    consent_at            TIMESTAMPTZ  NOT NULL,

    supplier_booking_id   VARCHAR(40),
    supplier_state        VARCHAR(20),
    supplier_price        NUMERIC(12, 3),
    voucher_url           TEXT,

    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_booking_status ON booking_request (status, created_at DESC);

CREATE TABLE booking_passenger (
    id          UUID PRIMARY KEY,
    booking_id  UUID        NOT NULL REFERENCES booking_request (id) ON DELETE CASCADE,
    position    INTEGER     NOT NULL,
    type        VARCHAR(5)  NOT NULL CHECK (type IN ('ADULT', 'CHILD')),
    civility    VARCHAR(5),
    first_name  VARCHAR(80) NOT NULL,
    last_name   VARCHAR(80) NOT NULL,
    age         INTEGER CHECK (age BETWEEN 0 AND 17)
);
CREATE INDEX idx_booking_passenger ON booking_passenger (booking_id, position);

-- Traceability: every action on a booking (who, when, what). No personal data in "details".
CREATE TABLE booking_event (
    id         UUID PRIMARY KEY,
    booking_id UUID         NOT NULL REFERENCES booking_request (id) ON DELETE CASCADE,
    at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actor      VARCHAR(160) NOT NULL,
    action     VARCHAR(40)  NOT NULL,
    details    VARCHAR(1000)
);
CREATE INDEX idx_booking_event ON booking_event (booking_id, at);
