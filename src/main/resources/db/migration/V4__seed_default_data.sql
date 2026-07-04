create extension if not exists pgcrypto;

with seed_users (id, email, password, full_name) as (
  values
    ('00000000-0000-4000-8000-000000000101'::uuid, 'demo.user1@example.com', 'Password123', 'Demo User 1'),
    ('00000000-0000-4000-8000-000000000102'::uuid, 'demo.user2@example.com', 'Password123', 'Demo User 2'),
    ('00000000-0000-4000-8000-000000000103'::uuid, 'demo.user3@example.com', 'Password123', 'Demo User 3'),
    ('00000000-0000-4000-8000-000000000104'::uuid, 'demo.user4@example.com', 'Password123', 'Demo User 4'),
    ('00000000-0000-4000-8000-000000000105'::uuid, 'demo.user5@example.com', 'Password123', 'Demo User 5')
)
insert into users (id, email, password_hash, full_name, created_at, updated_at)
select id, email, crypt(password, gen_salt('bf', 12)), full_name, now(), now()
from seed_users
on conflict (id) do nothing;

insert into user_roles (user_id, role, created_at, updated_at)
values
  ('00000000-0000-4000-8000-000000000101'::uuid, 'USER', now(), now()),
  ('00000000-0000-4000-8000-000000000102'::uuid, 'USER', now(), now()),
  ('00000000-0000-4000-8000-000000000103'::uuid, 'USER', now(), now()),
  ('00000000-0000-4000-8000-000000000104'::uuid, 'USER', now(), now()),
  ('00000000-0000-4000-8000-000000000105'::uuid, 'USER', now(), now())
on conflict do nothing;

insert into notification_preferences (id, user_id, is_enabled, created_at, updated_at)
values
  ('00000000-0000-4000-8000-000000000201'::uuid, '00000000-0000-4000-8000-000000000101'::uuid, true, now(), now()),
  ('00000000-0000-4000-8000-000000000202'::uuid, '00000000-0000-4000-8000-000000000102'::uuid, true, now(), now()),
  ('00000000-0000-4000-8000-000000000203'::uuid, '00000000-0000-4000-8000-000000000103'::uuid, false, now(), now()),
  ('00000000-0000-4000-8000-000000000204'::uuid, '00000000-0000-4000-8000-000000000104'::uuid, true, now(), now()),
  ('00000000-0000-4000-8000-000000000205'::uuid, '00000000-0000-4000-8000-000000000105'::uuid, false, now(), now())
on conflict (user_id) do nothing;

insert into locations (id, name, type, country, city, address, latitude, longitude)
values
  ('00000000-0000-4000-8000-000000000301'::uuid, 'Sheremetyevo International Airport', 'AIRPORT', 'Russia', 'Moscow', 'Khimki, Moscow Oblast', 55.9726, 37.4146),
  ('00000000-0000-4000-8000-000000000302'::uuid, 'Pulkovo Airport', 'AIRPORT', 'Russia', 'Saint Petersburg', 'Pulkovskoye Hwy, 41', 59.8003, 30.2625),
  ('00000000-0000-4000-8000-000000000303'::uuid, 'Sochi Railway Station', 'TRAIN_STATION', 'Russia', 'Sochi', 'Gorkogo St, 56', 43.5915, 39.7270),
  ('00000000-0000-4000-8000-000000000304'::uuid, 'Kazan Kremlin', 'ATTRACTION', 'Russia', 'Kazan', 'Kremlin St', 55.7998, 49.1052),
  ('00000000-0000-4000-8000-000000000305'::uuid, 'Rosa Khutor Resort', 'HOTEL', 'Russia', 'Sochi', 'Esto-Sadok', 43.6825, 40.2049)
on conflict (id) do nothing;

insert into providers (id, name, type, website, support_contact)
values
  ('00000000-0000-4000-8000-000000000401'::uuid, 'Demo Air', 'AIRLINE', 'https://demo-air.example.com', 'support@demo-air.example.com'),
  ('00000000-0000-4000-8000-000000000402'::uuid, 'North Rail', 'TRANSPORT_COMPANY', 'https://north-rail.example.com', 'support@north-rail.example.com'),
  ('00000000-0000-4000-8000-000000000403'::uuid, 'City Hotels', 'HOTEL', 'https://city-hotels.example.com', 'support@city-hotels.example.com'),
  ('00000000-0000-4000-8000-000000000404'::uuid, 'Travel Plus', 'TOUR_COMPANY', 'https://travel-plus.example.com', 'support@travel-plus.example.com'),
  ('00000000-0000-4000-8000-000000000405'::uuid, 'Go Booking', 'BOOKING_PLATFORM', 'https://go-booking.example.com', 'support@go-booking.example.com')
on conflict (id) do nothing;

insert into trips (id, user_id, title, start_date, end_date, status, created_at, updated_at)
values
  ('00000000-0000-4000-8000-000000000501'::uuid, '00000000-0000-4000-8000-000000000101'::uuid, 'Moscow Business Weekend', '2026-08-01', '2026-08-03', 'PLANNED', now(), now()),
  ('00000000-0000-4000-8000-000000000502'::uuid, '00000000-0000-4000-8000-000000000102'::uuid, 'Saint Petersburg Museums', '2026-08-10', '2026-08-14', 'PLANNED', now(), now()),
  ('00000000-0000-4000-8000-000000000503'::uuid, '00000000-0000-4000-8000-000000000103'::uuid, 'Sochi Summer Break', '2026-09-01', '2026-09-07', 'ACTIVE', now(), now()),
  ('00000000-0000-4000-8000-000000000504'::uuid, '00000000-0000-4000-8000-000000000104'::uuid, 'Kazan Long Weekend', '2026-10-02', '2026-10-05', 'PLANNED', now(), now()),
  ('00000000-0000-4000-8000-000000000505'::uuid, '00000000-0000-4000-8000-000000000105'::uuid, 'Rosa Khutor Ski Plan', '2027-01-12', '2027-01-18', 'PLANNED', now(), now())
on conflict (id) do nothing;

insert into trip_locations (id, trip_id, location_id, visit_order, arrival_date, departure_date)
values
  ('00000000-0000-4000-8000-000000000601'::uuid, '00000000-0000-4000-8000-000000000501'::uuid, '00000000-0000-4000-8000-000000000301'::uuid, 1, '2026-08-01 10:00:00+03', '2026-08-03 18:00:00+03'),
  ('00000000-0000-4000-8000-000000000602'::uuid, '00000000-0000-4000-8000-000000000502'::uuid, '00000000-0000-4000-8000-000000000302'::uuid, 1, '2026-08-10 09:00:00+03', '2026-08-14 20:00:00+03'),
  ('00000000-0000-4000-8000-000000000603'::uuid, '00000000-0000-4000-8000-000000000503'::uuid, '00000000-0000-4000-8000-000000000303'::uuid, 1, '2026-09-01 08:00:00+03', '2026-09-07 21:00:00+03'),
  ('00000000-0000-4000-8000-000000000604'::uuid, '00000000-0000-4000-8000-000000000504'::uuid, '00000000-0000-4000-8000-000000000304'::uuid, 1, '2026-10-02 12:00:00+03', '2026-10-05 16:00:00+03'),
  ('00000000-0000-4000-8000-000000000605'::uuid, '00000000-0000-4000-8000-000000000505'::uuid, '00000000-0000-4000-8000-000000000305'::uuid, 1, '2027-01-12 14:00:00+03', '2027-01-18 11:00:00+03')
on conflict (id) do nothing;

insert into orders (
  id, user_id, trip_id, provider_id, service_type, external_order_id, title, status,
  price_amount, price_currency, start_datetime, end_datetime, departure_location_id, arrival_location_id,
  created_at, updated_at
)
values
  ('00000000-0000-4000-8000-000000000701'::uuid, '00000000-0000-4000-8000-000000000101'::uuid, '00000000-0000-4000-8000-000000000501'::uuid, '00000000-0000-4000-8000-000000000401'::uuid, 'FLIGHT', 'DA-1001', 'Flight to Moscow', 'CONFIRMED', 12500.00, 'RUB', '2026-08-01 10:00:00+03', '2026-08-01 12:00:00+03', '00000000-0000-4000-8000-000000000302'::uuid, '00000000-0000-4000-8000-000000000301'::uuid, now(), now()),
  ('00000000-0000-4000-8000-000000000702'::uuid, '00000000-0000-4000-8000-000000000102'::uuid, '00000000-0000-4000-8000-000000000502'::uuid, '00000000-0000-4000-8000-000000000402'::uuid, 'TRAIN', 'NR-2002', 'Train to Saint Petersburg', 'CONFIRMED', 4800.00, 'RUB', '2026-08-10 09:00:00+03', '2026-08-10 13:00:00+03', '00000000-0000-4000-8000-000000000301'::uuid, '00000000-0000-4000-8000-000000000302'::uuid, now(), now()),
  ('00000000-0000-4000-8000-000000000703'::uuid, '00000000-0000-4000-8000-000000000103'::uuid, '00000000-0000-4000-8000-000000000503'::uuid, '00000000-0000-4000-8000-000000000403'::uuid, 'HOTEL', 'CH-3003', 'Sochi hotel booking', 'PENDING_VERIFICATION', 42000.00, 'RUB', '2026-09-01 15:00:00+03', '2026-09-07 12:00:00+03', null, '00000000-0000-4000-8000-000000000305'::uuid, now(), now()),
  ('00000000-0000-4000-8000-000000000704'::uuid, '00000000-0000-4000-8000-000000000104'::uuid, '00000000-0000-4000-8000-000000000504'::uuid, '00000000-0000-4000-8000-000000000404'::uuid, 'TOUR', 'TP-4004', 'Kazan city tour', 'CONFIRMED', 7200.00, 'RUB', '2026-10-03 11:00:00+03', '2026-10-03 16:00:00+03', '00000000-0000-4000-8000-000000000304'::uuid, '00000000-0000-4000-8000-000000000304'::uuid, now(), now()),
  ('00000000-0000-4000-8000-000000000705'::uuid, '00000000-0000-4000-8000-000000000105'::uuid, '00000000-0000-4000-8000-000000000505'::uuid, '00000000-0000-4000-8000-000000000405'::uuid, 'INSURANCE', 'GB-5005', 'Travel insurance', 'CONFIRMED', 3500.00, 'RUB', '2027-01-12 00:00:00+03', '2027-01-18 23:59:00+03', null, null, now(), now())
on conflict (id) do nothing;

insert into order_files (id, order_id, file_url, file_type, parsed_data, uploaded_at)
values
  ('00000000-0000-4000-8000-000000000801'::uuid, '00000000-0000-4000-8000-000000000701'::uuid, 'https://files.example.com/orders/DA-1001.pdf', 'PDF', '{"source":"seed"}'::jsonb, now()),
  ('00000000-0000-4000-8000-000000000802'::uuid, '00000000-0000-4000-8000-000000000702'::uuid, 'https://files.example.com/orders/NR-2002.pdf', 'PDF', '{"source":"seed"}'::jsonb, now()),
  ('00000000-0000-4000-8000-000000000803'::uuid, '00000000-0000-4000-8000-000000000703'::uuid, 'https://files.example.com/orders/CH-3003.json', 'JSON', '{"source":"seed"}'::jsonb, now()),
  ('00000000-0000-4000-8000-000000000804'::uuid, '00000000-0000-4000-8000-000000000704'::uuid, 'https://files.example.com/orders/TP-4004.eml', 'EMAIL', '{"source":"seed"}'::jsonb, now()),
  ('00000000-0000-4000-8000-000000000805'::uuid, '00000000-0000-4000-8000-000000000705'::uuid, 'https://files.example.com/orders/GB-5005.png', 'IMAGE', '{"source":"seed"}'::jsonb, now())
on conflict (id) do nothing;

insert into order_status_events (id, order_id, old_status, new_status, reason, payload, source, created_at)
values
  ('00000000-0000-4000-8000-000000000901'::uuid, '00000000-0000-4000-8000-000000000701'::uuid, 'PENDING_VERIFICATION', 'CONFIRMED', 'Seed confirmation', '{"source":"seed"}'::jsonb, 'system', now()),
  ('00000000-0000-4000-8000-000000000902'::uuid, '00000000-0000-4000-8000-000000000702'::uuid, 'PENDING_VERIFICATION', 'CONFIRMED', 'Seed confirmation', '{"source":"seed"}'::jsonb, 'system', now()),
  ('00000000-0000-4000-8000-000000000903'::uuid, '00000000-0000-4000-8000-000000000703'::uuid, null, 'PENDING_VERIFICATION', 'Seed import', '{"source":"seed"}'::jsonb, 'user_edit', now()),
  ('00000000-0000-4000-8000-000000000904'::uuid, '00000000-0000-4000-8000-000000000704'::uuid, 'PENDING_VERIFICATION', 'CONFIRMED', 'Seed confirmation', '{"source":"seed"}'::jsonb, 'admin_simulation', now()),
  ('00000000-0000-4000-8000-000000000905'::uuid, '00000000-0000-4000-8000-000000000705'::uuid, 'PENDING_VERIFICATION', 'CONFIRMED', 'Seed confirmation', '{"source":"seed"}'::jsonb, 'system', now())
on conflict (id) do nothing;

insert into additional_services (id, title, description, service_type, provider_id, location_id, price_amount, price_currency, is_active)
values
  ('00000000-0000-4000-8000-000000001001'::uuid, 'Priority boarding', 'Priority boarding for demo flights', 'FLIGHT', '00000000-0000-4000-8000-000000000401'::uuid, '00000000-0000-4000-8000-000000000301'::uuid, 1500, 'RUB', true),
  ('00000000-0000-4000-8000-000000001002'::uuid, 'Lounge access', 'Airport lounge access', 'LOUNGE', '00000000-0000-4000-8000-000000000401'::uuid, '00000000-0000-4000-8000-000000000301'::uuid, 3200, 'RUB', true),
  ('00000000-0000-4000-8000-000000001003'::uuid, 'Hotel breakfast', 'Breakfast add-on for hotel stays', 'HOTEL', '00000000-0000-4000-8000-000000000403'::uuid, '00000000-0000-4000-8000-000000000305'::uuid, 900, 'RUB', true),
  ('00000000-0000-4000-8000-000000001004'::uuid, 'City transfer', 'Transfer from arrival point to hotel', 'TAXI', '00000000-0000-4000-8000-000000000405'::uuid, '00000000-0000-4000-8000-000000000302'::uuid, 2500, 'RUB', true),
  ('00000000-0000-4000-8000-000000001005'::uuid, 'Extra baggage', 'One extra checked bag', 'EXTRA_BAGGAGE', '00000000-0000-4000-8000-000000000401'::uuid, null, 4000, 'RUB', true)
on conflict (id) do nothing;

insert into reviews (id, user_id, target_type, target_id, rating, text, created_at, updated_at)
values
  ('00000000-0000-4000-8000-000000001101'::uuid, '00000000-0000-4000-8000-000000000101'::uuid, 'PROVIDER', '00000000-0000-4000-8000-000000000401'::uuid, 5, 'Fast and clear service.', now(), now()),
  ('00000000-0000-4000-8000-000000001102'::uuid, '00000000-0000-4000-8000-000000000102'::uuid, 'LOCATION', '00000000-0000-4000-8000-000000000302'::uuid, 4, 'Convenient location.', now(), now()),
  ('00000000-0000-4000-8000-000000001103'::uuid, '00000000-0000-4000-8000-000000000103'::uuid, 'ORDER', '00000000-0000-4000-8000-000000000703'::uuid, 4, 'Booking is waiting for confirmation.', now(), now()),
  ('00000000-0000-4000-8000-000000001104'::uuid, '00000000-0000-4000-8000-000000000104'::uuid, 'ADDITIONAL_SERVICE', '00000000-0000-4000-8000-000000001004'::uuid, 5, 'Transfer was useful.', now(), now()),
  ('00000000-0000-4000-8000-000000001105'::uuid, '00000000-0000-4000-8000-000000000105'::uuid, 'PROVIDER', '00000000-0000-4000-8000-000000000405'::uuid, 4, 'Simple booking flow.', now(), now())
on conflict (id) do nothing;

insert into achievements (id, code, title, description, condition_type, condition_value, icon_url, created_at, updated_at)
values
  ('00000000-0000-4000-8000-000000001201'::uuid, 'first_trip', 'First trip', 'Create your first trip.', 'TRIPS_COUNT', 1, 'https://assets.example.com/achievements/first-trip.svg', now(), now()),
  ('00000000-0000-4000-8000-000000001202'::uuid, 'five_trips', 'Five trips', 'Create five trips.', 'TRIPS_COUNT', 5, 'https://assets.example.com/achievements/five-trips.svg', now(), now()),
  ('00000000-0000-4000-8000-000000001203'::uuid, 'three_orders', 'Three orders', 'Create three orders.', 'ORDERS_COUNT', 3, 'https://assets.example.com/achievements/three-orders.svg', now(), now()),
  ('00000000-0000-4000-8000-000000001204'::uuid, 'reviewer', 'Reviewer', 'Leave your first review.', 'REVIEWS_COUNT', 1, 'https://assets.example.com/achievements/reviewer.svg', now(), now()),
  ('00000000-0000-4000-8000-000000001205'::uuid, 'spender_50000', 'Big planner', 'Plan spending over 50000.', 'SPENDING_AMOUNT', 50000, 'https://assets.example.com/achievements/spender.svg', now(), now())
on conflict (id) do nothing;

insert into user_achievements (id, user_id, achievement_id, unlocked_at, created_at, updated_at)
values
  ('00000000-0000-4000-8000-000000001301'::uuid, '00000000-0000-4000-8000-000000000101'::uuid, '00000000-0000-4000-8000-000000001201'::uuid, now(), now(), now()),
  ('00000000-0000-4000-8000-000000001302'::uuid, '00000000-0000-4000-8000-000000000102'::uuid, '00000000-0000-4000-8000-000000001201'::uuid, now(), now(), now()),
  ('00000000-0000-4000-8000-000000001303'::uuid, '00000000-0000-4000-8000-000000000103'::uuid, '00000000-0000-4000-8000-000000001203'::uuid, now(), now(), now()),
  ('00000000-0000-4000-8000-000000001304'::uuid, '00000000-0000-4000-8000-000000000104'::uuid, '00000000-0000-4000-8000-000000001204'::uuid, now(), now(), now()),
  ('00000000-0000-4000-8000-000000001305'::uuid, '00000000-0000-4000-8000-000000000105'::uuid, '00000000-0000-4000-8000-000000001201'::uuid, now(), now(), now())
on conflict (user_id, achievement_id) do nothing;

insert into user_notifications (id, user_id, order_id, type, title, body, is_read, sent_at, created_at, updated_at)
values
  ('00000000-0000-4000-8000-000000001401'::uuid, '00000000-0000-4000-8000-000000000101'::uuid, '00000000-0000-4000-8000-000000000701'::uuid, 'STATUS_CHANGE', 'Order confirmed', 'Your flight order has been confirmed.', false, now(), now(), now()),
  ('00000000-0000-4000-8000-000000001402'::uuid, '00000000-0000-4000-8000-000000000102'::uuid, '00000000-0000-4000-8000-000000000702'::uuid, 'STATUS_CHANGE', 'Order confirmed', 'Your train order has been confirmed.', true, now(), now(), now()),
  ('00000000-0000-4000-8000-000000001403'::uuid, '00000000-0000-4000-8000-000000000103'::uuid, '00000000-0000-4000-8000-000000000703'::uuid, 'REMINDER', 'Hotel check-in', 'Do not forget your hotel check-in.', false, now(), now(), now()),
  ('00000000-0000-4000-8000-000000001404'::uuid, '00000000-0000-4000-8000-000000000104'::uuid, '00000000-0000-4000-8000-000000000704'::uuid, 'GENERAL', 'Tour starts soon', 'Your Kazan city tour starts tomorrow.', false, now(), now(), now()),
  ('00000000-0000-4000-8000-000000001405'::uuid, '00000000-0000-4000-8000-000000000105'::uuid, '00000000-0000-4000-8000-000000000705'::uuid, 'PROMO', 'Winter offer', 'Extra services are available for your ski trip.', true, now(), now(), now())
on conflict (id) do nothing;

insert into auth_sessions (id, user_id, refresh_token_hash, expires_at, revoked_at, created_at, updated_at)
values
  ('00000000-0000-4000-8000-000000001501'::uuid, '00000000-0000-4000-8000-000000000101'::uuid, 'seed-refresh-token-hash-1', now() + interval '30 days', null, now(), now()),
  ('00000000-0000-4000-8000-000000001502'::uuid, '00000000-0000-4000-8000-000000000102'::uuid, 'seed-refresh-token-hash-2', now() + interval '30 days', null, now(), now()),
  ('00000000-0000-4000-8000-000000001503'::uuid, '00000000-0000-4000-8000-000000000103'::uuid, 'seed-refresh-token-hash-3', now() + interval '30 days', null, now(), now()),
  ('00000000-0000-4000-8000-000000001504'::uuid, '00000000-0000-4000-8000-000000000104'::uuid, 'seed-refresh-token-hash-4', now() + interval '30 days', null, now(), now()),
  ('00000000-0000-4000-8000-000000001505'::uuid, '00000000-0000-4000-8000-000000000105'::uuid, 'seed-refresh-token-hash-5', now() + interval '30 days', null, now(), now())
on conflict (id) do nothing;
