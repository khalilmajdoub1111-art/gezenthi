-- V8: notifications sent about bookings (e-mail, WhatsApp).

-- WhatsApp needs explicit opt-in from the customer (Meta policy).
ALTER TABLE booking_request ADD COLUMN whatsapp_opt_in BOOLEAN NOT NULL DEFAULT FALSE;

-- Traceability of every message: what, to whom (MASKED), result. Never the message body.
CREATE TABLE notification_log (
    id          UUID PRIMARY KEY,
    booking_id  UUID         REFERENCES booking_request (id) ON DELETE CASCADE,
    channel     VARCHAR(10)  NOT NULL CHECK (channel IN ('EMAIL', 'WHATSAPP')),
    audience    VARCHAR(10)  NOT NULL CHECK (audience IN ('CUSTOMER', 'AGENCY')),
    template    VARCHAR(40)  NOT NULL,
    recipient   VARCHAR(160) NOT NULL,
    status      VARCHAR(10)  NOT NULL CHECK (status IN ('SENT', 'FAILED', 'SKIPPED')),
    error       VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_notification_booking ON notification_log (booking_id, created_at);
