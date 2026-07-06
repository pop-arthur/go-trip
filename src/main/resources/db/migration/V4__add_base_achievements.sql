-- Базовые ачивки для системы достижений
INSERT INTO achievements (id, code, title, description, condition_type, condition_value, icon_url, created_at, updated_at)
VALUES
  (gen_random_uuid(), 'FIRST_TRIP', 'First Trip', 'Create your first trip', 'TRIPS_COUNT', 1, 'https://example.com/icons/first-trip.svg', now(), now()),
  (gen_random_uuid(), 'EXPLORER', 'Explorer', 'Visit 5 different countries', 'COUNTRIES_COUNT', 5, 'https://example.com/icons/explorer.svg', now(), now()),
  (gen_random_uuid(), 'GLOBETROTTER', 'Globetrotter', 'Visit 10 different countries', 'COUNTRIES_COUNT', 10, 'https://example.com/icons/globetrotter.svg', now(), now()),
  (gen_random_uuid(), 'FIRST_ORDER', 'First Order', 'Add your first order', 'ORDERS_COUNT', 1, 'https://example.com/icons/first-order.svg', now(), now()),
  (gen_random_uuid(), 'ORGANIZER', 'Organizer', 'Add 5 orders', 'ORDERS_COUNT', 5, 'https://example.com/icons/organizer.svg', now(), now()),
  (gen_random_uuid(), 'REVIEWER', 'Reviewer', 'Leave your first review', 'REVIEWS_COUNT', 1, 'https://example.com/icons/reviewer.svg', now(), now()),
  (gen_random_uuid(), 'CRITIC', 'Critic', 'Leave 5 reviews', 'REVIEWS_COUNT', 5, 'https://example.com/icons/critic.svg', now(), now()),
  (gen_random_uuid(), 'BIG_SPENDER', 'Big Spender', 'Spend over $1000', 'SPENDING_AMOUNT', 1000, 'https://example.com/icons/big-spender.svg', now(), now()),
  (gen_random_uuid(), 'MEGA_SPENDER', 'Mega Spender', 'Spend over $5000', 'SPENDING_AMOUNT', 5000, 'https://example.com/icons/mega-spender.svg', now(), now())
ON CONFLICT (code) DO NOTHING;