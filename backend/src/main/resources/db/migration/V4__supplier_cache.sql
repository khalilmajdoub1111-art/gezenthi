-- V4: hotel suppliers (B2B APIs such as TunisiaBeds / OS-TRAVEL).
-- Suppliers only allow searching by THEIR hotel ids, so their static catalogue
-- (hotel ids, names, cities) is synchronised and stored here. Prices are never
-- stored: they are fetched live at search time.

CREATE TABLE supplier_hotel (
    id               UUID PRIMARY KEY,
    supplier_code    VARCHAR(40)  NOT NULL,
    external_id      VARCHAR(40)  NOT NULL,
    name             VARCHAR(200) NOT NULL,
    city_external_id VARCHAR(40),
    city_name        VARCHAR(120) NOT NULL,
    stars            INTEGER,
    image            TEXT,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    synced_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_supplier_hotel UNIQUE (supplier_code, external_id)
);
CREATE INDEX idx_supplier_hotel_city ON supplier_hotel (supplier_code, lower(city_name));

-- Traceability: every synchronisation run (manual or scheduled) is recorded.
CREATE TABLE supplier_sync_log (
    id            UUID PRIMARY KEY,
    supplier_code VARCHAR(40)  NOT NULL,
    started_at    TIMESTAMPTZ  NOT NULL,
    finished_at   TIMESTAMPTZ,
    status        VARCHAR(20)  NOT NULL CHECK (status IN ('RUNNING', 'SUCCESS', 'FAILED')),
    hotels        INTEGER      NOT NULL DEFAULT 0,
    message       VARCHAR(500),
    triggered_by  VARCHAR(160)
);
CREATE INDEX idx_supplier_sync_log ON supplier_sync_log (supplier_code, started_at DESC);
