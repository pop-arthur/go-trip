-- Типы enum
create type location_type as enum (
  'COUNTRY', 'CITY', 'AIRPORT', 'TRAIN_STATION', 'BUS_STATION',
  'PORT', 'HOTEL', 'MEETING_POINT', 'ATTRACTION', 'OTHER'
);

create type trip_status as enum (
  'PLANNED', 'ACTIVE', 'COMPLETED', 'CANCELLED'
);

create type provider_type as enum (
  'AIRLINE', 'HOTEL', 'TOUR_COMPANY', 'TRANSPORT_COMPANY',
  'BOOKING_PLATFORM', 'INSURANCE_COMPANY', 'OTHER'
);

create type service_type as enum (
  'FLIGHT', 'TRAIN', 'BUS', 'HOTEL', 'TOUR', 'CAR_RENTAL',
  'INSURANCE', 'TAXI', 'ESIM', 'LOUNGE', 'EXTRA_BAGGAGE', 'OTHER'
);

-- 1. Users
create table users (
  id            uuid primary key,
  email         varchar(255) not null unique,
  password_hash varchar(255) not null,
  full_name     varchar(255),
  created_at    timestamptz not null default now(),
  updated_at    timestamptz not null default now()
);

-- 2. User roles
create table user_roles (
  user_id    uuid not null references users(id) on delete cascade,
  role       text not null check (role in ('USER', 'ADMIN')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  primary key (user_id, role)
);

-- 3. Notification preferences (one per user)
create table notification_preferences (
  id         uuid primary key,
  user_id    uuid not null unique references users(id) on delete cascade,
  is_enabled boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- 4. Locations
create table locations (
  id         uuid primary key,
  name       varchar(255) not null,
  type       location_type not null,
  country    varchar(255),
  city       varchar(255),
  address    text,
  latitude   double precision,
  longitude  double precision
);
create index idx_locations_type on locations(type);
create index idx_locations_lower_country on locations(lower(country));
create index idx_locations_lower_city on locations(lower(city));

-- 5. Providers
create table providers (
  id              uuid primary key,
  name            varchar(255) not null,
  type            provider_type not null,
  website         varchar(2048),
  support_contact varchar(255)
);
create unique index uq_providers_lower_name on providers(lower(name));
create index idx_providers_type on providers(type);

-- 6. Trips
create table trips (
  id         uuid primary key,
  user_id    uuid not null references users(id) on delete cascade,
  title      varchar(255) not null,
  start_date date,
  end_date   date,
  status     trip_status not null default 'PLANNED',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint chk_trips_date_range check (
    start_date is null or end_date is null or start_date <= end_date
  )
);
create index idx_trips_user_id on trips(user_id);
create index idx_trips_status on trips(status);

-- 7. Trip locations (route)
create table trip_locations (
  id             uuid primary key,
  trip_id        uuid not null references trips(id) on delete cascade,
  location_id    uuid not null references locations(id),
  visit_order    integer not null,
  arrival_date   timestamptz,
  departure_date timestamptz,
  constraint chk_trip_locations_visit_order_positive check (visit_order > 0),
  constraint chk_trip_locations_date_range check (
    arrival_date is null or departure_date is null or arrival_date <= departure_date
  ),
  constraint uq_trip_locations_trip_visit_order unique (trip_id, visit_order)
);
create index idx_trip_locations_trip_id on trip_locations(trip_id);
create index idx_trip_locations_location_id on trip_locations(location_id);

-- 8. Orders
create table orders (
  id                    uuid primary key,
  user_id               uuid not null references users(id) on delete cascade,
  trip_id               uuid not null references trips(id) on delete cascade,
  provider_id           uuid references providers(id),
  service_type          service_type not null,
  external_order_id     varchar(255),
  title                 varchar(255) not null,
  status                text not null default 'PENDING_VERIFICATION' check (status in (
    'PENDING_VERIFICATION', 'CONFIRMED', 'DELAYED', 'CANCELLED', 'COMPLETED', 'REFUND_PENDING', 'REFUNDED'
  )),
  price_amount          decimal(12,2),
  price_currency        varchar(10),
  start_datetime        timestamptz,
  end_datetime          timestamptz,
  departure_location_id uuid references locations(id),
  arrival_location_id   uuid references locations(id),
  created_at            timestamptz not null default now(),
  updated_at            timestamptz not null default now()
);
create index idx_orders_user_id on orders(user_id);
create index idx_orders_trip_id on orders(trip_id);
create index idx_orders_provider_id on orders(provider_id);
create index idx_orders_departure_location on orders(departure_location_id);
create index idx_orders_arrival_location on orders(arrival_location_id);

-- 9. Order files
create table order_files (
  id          uuid primary key,
  order_id    uuid not null references orders(id) on delete cascade,
  file_url    text not null,
  file_type   text not null check (file_type in ('PDF', 'IMAGE', 'EMAIL', 'JSON', 'OTHER')),
  parsed_data jsonb,
  uploaded_at timestamptz not null default now()
);
create index idx_order_files_order_id on order_files(order_id);

-- 10. Order status events (history)
create table order_status_events (
  id          uuid primary key,
  order_id    uuid not null references orders(id) on delete cascade,
  old_status  text check (old_status in (
    'PENDING_VERIFICATION', 'CONFIRMED', 'DELAYED', 'CANCELLED', 'COMPLETED', 'REFUND_PENDING', 'REFUNDED'
  )),
  new_status  text not null check (new_status in (
    'PENDING_VERIFICATION', 'CONFIRMED', 'DELAYED', 'CANCELLED', 'COMPLETED', 'REFUND_PENDING', 'REFUNDED'
  )),
  reason      text,
  payload     jsonb,
  source      text not null default 'system' check (source in ('system', 'admin_simulation', 'user_edit')),
  created_at  timestamptz not null default now()
);
create index idx_order_status_events_order_id on order_status_events(order_id);

-- 11. Additional services (catalog)
create table additional_services (
  id              uuid primary key,
  title           varchar(255) not null,
  description     text,
  service_type    service_type not null,
  provider_id     uuid references providers(id),
  location_id     uuid references locations(id),
  price_amount    double precision,
  price_currency  varchar(10),
  is_active       boolean not null default true,
  constraint chk_additional_services_price_nonnegative check (
    price_amount is null or price_amount >= 0
  )
);
create index idx_additional_services_service_type on additional_services(service_type);
create index idx_additional_services_provider_id on additional_services(provider_id);
create index idx_additional_services_location_id on additional_services(location_id);
create index idx_additional_services_is_active on additional_services(is_active);
create index idx_additional_services_lower_title on additional_services(lower(title));

-- 12. Reviews (polymorphic)
create table reviews (
  id          uuid primary key,
  user_id     uuid not null references users(id) on delete cascade,
  target_type text not null check (target_type in ('PROVIDER', 'LOCATION', 'ORDER', 'ADDITIONAL_SERVICE')),
  target_id   uuid not null,
  rating      int not null check (rating >= 1 and rating <= 5),
  text        text,
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now()
);
create index idx_reviews_target on reviews(target_type, target_id);

-- 13. Achievements (catalog)
create table achievements (
  id              uuid primary key,
  code            text not null unique,
  title           text not null,
  description     text,
  condition_type  text not null check (condition_type in (
    'TRIPS_COUNT', 'COUNTRIES_COUNT', 'ORDERS_COUNT', 'REVIEWS_COUNT', 'SPENDING_AMOUNT'
  )),
  condition_value int not null,
  icon_url        text,
  created_at      timestamptz not null default now(),
  updated_at      timestamptz not null default now()
);

-- 14. User achievements
create table user_achievements (
  id             uuid primary key,
  user_id        uuid not null references users(id) on delete cascade,
  achievement_id uuid not null references achievements(id) on delete cascade,
  unlocked_at    timestamptz not null default now(),
  created_at     timestamptz not null default now(),
  updated_at     timestamptz not null default now(),
  unique(user_id, achievement_id)
);
create index idx_user_achievements_user_id on user_achievements(user_id);

-- 15. User notifications (history)
create table user_notifications (
  id          uuid primary key,
  user_id     uuid not null references users(id) on delete cascade,
  order_id    uuid references orders(id) on delete set null,
  type        text not null check (type in ('STATUS_CHANGE', 'REMINDER', 'GENERAL', 'PROMO', 'OTHER')),
  title       text not null,
  body        text not null,
  is_read     boolean not null default false,
  sent_at     timestamptz not null default now(),
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now()
);
create index idx_user_notifications_user_id on user_notifications(user_id);
create index idx_user_notifications_order_id on user_notifications(order_id);
create index idx_user_notifications_sent_at on user_notifications(sent_at);
create index idx_user_notifications_is_read on user_notifications(is_read);
