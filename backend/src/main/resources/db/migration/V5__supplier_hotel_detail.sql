-- V5: rich hotel content from suppliers (HotelDetail), fetched on demand and cached.
-- description/notes are stored ALREADY SANITISED (safe HTML subset).
CREATE TABLE supplier_hotel_detail (
    supplier_hotel_id UUID PRIMARY KEY REFERENCES supplier_hotel (id) ON DELETE CASCADE,
    description TEXT,
    notes       TEXT,
    address     VARCHAR(300),
    latitude    VARCHAR(30),
    longitude   VARCHAR(30),
    check_in    VARCHAR(30),
    check_out   VARCHAR(30),
    type        VARCHAR(60),
    images      TEXT[] NOT NULL DEFAULT '{}',
    facilities  TEXT[] NOT NULL DEFAULT '{}',
    tags        TEXT[] NOT NULL DEFAULT '{}',
    themes      TEXT[] NOT NULL DEFAULT '{}',
    boards      TEXT[] NOT NULL DEFAULT '{}',
    fetched_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
