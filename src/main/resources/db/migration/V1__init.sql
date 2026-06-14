CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    email      TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    full_name  TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE user_roles (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role       TEXT NOT NULL CHECK (role IN ('USER', 'ADMIN')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE notification_preferences (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE locations (
    id         BIGSERIAL PRIMARY KEY,
    name       TEXT NOT NULL,
    type       TEXT NOT NULL CHECK (type IN ('COUNTRY', 'CITY', 'AIRPORT', 'TRAIN_STATION', 'BUS_STATION', 'PORT', 'HOTEL', 'MEETING_POINT', 'ATTRACTION', 'OTHER')),
    country    TEXT,
    city       TEXT,
    address    TEXT,
    latitude   DOUBLE PRECISION,
    longitude  DOUBLE PRECISION,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE providers (
    id              BIGSERIAL PRIMARY KEY,
    name            TEXT NOT NULL,
    type            TEXT NOT NULL CHECK (type IN ('AIRLINE', 'HOTEL', 'TOUR_COMPANY', 'TRANSPORT_COMPANY', 'BOOKING_PLATFORM', 'INSURANCE_COMPANY', 'OTHER')),
    website         TEXT,
    support_contact TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE trips (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title      TEXT NOT NULL,
    start_date DATE,
    end_date   DATE,
    status     TEXT NOT NULL DEFAULT 'PLANNED' CHECK (status IN ('PLANNED', 'ACTIVE', 'COMPLETED', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE trip_locations (
    id             BIGSERIAL PRIMARY KEY,
    trip_id        BIGINT NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    location_id    BIGINT NOT NULL REFERENCES locations(id),
    visit_order    INT NOT NULL,
    arrival_date   TIMESTAMPTZ,
    departure_date TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(trip_id, visit_order)
);

CREATE TABLE orders (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    trip_id              BIGINT NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    provider_id          BIGINT REFERENCES providers(id),
    service_type         TEXT NOT NULL CHECK (service_type IN ('FLIGHT', 'TRAIN', 'BUS', 'HOTEL', 'TOUR', 'CAR_RENTAL', 'INSURANCE', 'TAXI', 'ESIM', 'LOUNGE', 'EXTRA_BAGGAGE', 'OTHER')),
    external_order_id    TEXT,
    title                TEXT NOT NULL,
    status               TEXT NOT NULL DEFAULT 'PENDING_VERIFICATION' CHECK (status IN ('PENDING_VERIFICATION', 'CONFIRMED', 'DELAYED', 'CANCELLED', 'COMPLETED', 'REFUND_PENDING', 'REFUNDED')),
    price_amount         DECIMAL(12,2),
    price_currency       TEXT,
    start_datetime       TIMESTAMPTZ,
    end_datetime         TIMESTAMPTZ,
    departure_location_id BIGINT REFERENCES locations(id),
    arrival_location_id   BIGINT REFERENCES locations(id),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE order_files (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    file_url    TEXT NOT NULL,
    file_type   TEXT NOT NULL CHECK (file_type IN ('PDF', 'IMAGE', 'EMAIL', 'JSON', 'OTHER')),
    parsed_data JSONB,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE order_status_events (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    old_status  TEXT CHECK (old_status IN ('PENDING_VERIFICATION', 'CONFIRMED', 'DELAYED', 'CANCELLED', 'COMPLETED', 'REFUND_PENDING', 'REFUNDED')),
    new_status  TEXT NOT NULL CHECK (new_status IN ('PENDING_VERIFICATION', 'CONFIRMED', 'DELAYED', 'CANCELLED', 'COMPLETED', 'REFUND_PENDING', 'REFUNDED')),
    reason      TEXT,
    payload     JSONB,
    source      TEXT NOT NULL DEFAULT 'system' CHECK (source IN ('system', 'admin_simulation', 'user_edit')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE additional_services (
    id            BIGSERIAL PRIMARY KEY,
    title         TEXT NOT NULL,
    description   TEXT,
    service_type  TEXT NOT NULL CHECK (service_type IN ('FLIGHT', 'TRAIN', 'BUS', 'HOTEL', 'TOUR', 'CAR_RENTAL', 'INSURANCE', 'TAXI', 'ESIM', 'LOUNGE', 'EXTRA_BAGGAGE', 'OTHER')),
    provider_id   BIGINT REFERENCES providers(id),
    location_id   BIGINT REFERENCES locations(id),
    price_amount  DECIMAL(12,2),
    price_currency TEXT,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE reviews (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_type TEXT NOT NULL CHECK (target_type IN ('PROVIDER', 'LOCATION', 'ORDER', 'ADDITIONAL_SERVICE')),
    target_id   BIGINT NOT NULL,
    rating      INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    text        TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_reviews_target ON reviews(target_type, target_id);

CREATE TABLE achievements (
    id              BIGSERIAL PRIMARY KEY,
    code            TEXT NOT NULL UNIQUE,
    title           TEXT NOT NULL,
    description     TEXT,
    condition_type  TEXT NOT NULL CHECK (condition_type IN ('TRIPS_COUNT', 'COUNTRIES_COUNT', 'ORDERS_COUNT', 'REVIEWS_COUNT', 'SPENDING_AMOUNT')),
    condition_value INT NOT NULL,
    icon_url        TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE user_achievements (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    achievement_id BIGINT NOT NULL REFERENCES achievements(id) ON DELETE CASCADE,
    unlocked_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, achievement_id)
);

CREATE INDEX idx_trips_user_id ON trips(user_id);
CREATE INDEX idx_trip_locations_trip_id ON trip_locations(trip_id);
CREATE INDEX idx_trip_locations_location_id ON trip_locations(location_id);
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_trip_id ON orders(trip_id);
CREATE INDEX idx_orders_provider_id ON orders(provider_id);
CREATE INDEX idx_orders_departure_location ON orders(departure_location_id);
CREATE INDEX idx_orders_arrival_location ON orders(arrival_location_id);
CREATE INDEX idx_order_files_order_id ON order_files(order_id);
CREATE INDEX idx_order_status_events_order_id ON order_status_events(order_id);
CREATE INDEX idx_additional_services_provider_id ON additional_services(provider_id);
CREATE INDEX idx_additional_services_location_id ON additional_services(location_id);
CREATE INDEX idx_user_achievements_user_id ON user_achievements(user_id);