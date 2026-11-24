-- V2: catalogue managed from the admin panel.
-- Every table: UUID primary key, published flag, audit timestamps.
-- CHECK constraints mirror the API validation so bad data can't get in by another path.

CREATE TABLE hotel (
    id              UUID PRIMARY KEY,
    published       BOOLEAN      NOT NULL DEFAULT TRUE,
    name            VARCHAR(160) NOT NULL,
    region          VARCHAR(10)  NOT NULL CHECK (region IN ('tunisie', 'monde')),
    city            VARCHAR(80)  NOT NULL,
    stars           INTEGER      NOT NULL CHECK (stars BETWEEN 3 AND 5),
    rating          NUMERIC(3,1) NOT NULL DEFAULT 0 CHECK (rating BETWEEN 0 AND 10),
    reviews         INTEGER      NOT NULL DEFAULT 0 CHECK (reviews >= 0),
    board           VARCHAR(40)  NOT NULL,
    price_per_night INTEGER      NOT NULL CHECK (price_per_night >= 0),
    old_price       INTEGER      CHECK (old_price >= 0),
    image           TEXT         NOT NULL,
    tags            TEXT[]       NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE trip (
    id            UUID PRIMARY KEY,
    published     BOOLEAN      NOT NULL DEFAULT TRUE,
    name          VARCHAR(160) NOT NULL,
    country       VARCHAR(80)  NOT NULL,
    duration_days INTEGER      NOT NULL CHECK (duration_days >= 1),
    price_from    INTEGER      NOT NULL CHECK (price_from >= 0),
    image         TEXT         NOT NULL,
    no_visa       BOOLEAN      NOT NULL DEFAULT FALSE,
    badge         VARCHAR(40),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE omra_program (
    id             UUID PRIMARY KEY,
    published      BOOLEAN      NOT NULL DEFAULT TRUE,
    title          VARCHAR(160) NOT NULL,
    duration_days  INTEGER      NOT NULL CHECK (duration_days >= 1),
    departure      VARCHAR(60)  NOT NULL,
    hotel_distance INTEGER      CHECK (hotel_distance >= 0),
    price_from     INTEGER      NOT NULL CHECK (price_from >= 0),
    image          TEXT         NOT NULL DEFAULT '',
    includes       TEXT[]       NOT NULL DEFAULT '{}',
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE flight_deal (
    id           UUID PRIMARY KEY,
    published    BOOLEAN     NOT NULL DEFAULT TRUE,
    city         VARCHAR(80) NOT NULL,
    airport_code VARCHAR(4)  NOT NULL,
    price_from   INTEGER     NOT NULL CHECK (price_from >= 0),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE service_offer (
    id          UUID PRIMARY KEY,
    published   BOOLEAN      NOT NULL DEFAULT TRUE,
    kind        VARCHAR(20)  NOT NULL CHECK (kind IN ('Visa', 'Ferry CTN', 'TRE')),
    title       VARCHAR(160) NOT NULL,
    price_from  INTEGER      CHECK (price_from >= 0),
    description VARCHAR(500) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Public site only reads published rows, newest first.
CREATE INDEX idx_hotel_published         ON hotel (published, created_at DESC);
CREATE INDEX idx_trip_published          ON trip (published, created_at DESC);
CREATE INDEX idx_omra_program_published  ON omra_program (published, created_at DESC);
CREATE INDEX idx_flight_deal_published   ON flight_deal (published, created_at DESC);
CREATE INDEX idx_service_offer_published ON service_offer (published, created_at DESC);
