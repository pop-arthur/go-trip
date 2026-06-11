# GoTrip Database

This directory contains the database entity relationship diagram:

- `er-diagram.png` - exported ER diagram image
- `er-diagram.drawio` - editable diagram source

The schema models users, trips, route locations, travel orders, service providers, add-on services, reviews, achievements, and notification settings.

## Tables

### `USERS`

Stores registered application users.

Each row should contain a unique email, the hashed password used for authentication, an optional full name, and audit timestamps. Other user-owned data, such as trips, reviews, roles, notification preferences, and achievements, references this table through `user_id`.

Main fields:

- `id` - primary key.
- `email` - unique login email.
- `password_hash` - password hash, never the plain password.
- `full_name` - display name for the user.
- `created_at`, `updated_at` - audit timestamps.

### `USER_ROLES`

Stores roles assigned to a user.

Each row links one user to one role. A user can have multiple rows when they have multiple roles, for example `USER` and `ADMIN`.

Main fields:

- `id` - primary key.
- `user_id` - references `USERS.id`.
- `role` - role name, such as `USER` or `ADMIN`.
- `created_at` - timestamp when the role was assigned.

### `NOTIFICATION_PREFERENCES`

Stores per-user notification settings.

Each row represents whether a user wants to receive the notification. For the current API, the supported channel is `PUSH`.

Main fields:

- `id` - primary key.
- `user_id` - references `USERS.id`.
- `is_enabled` - whether this preference is active.

### `TRIPS`

Stores user-created trips.

Each row is a trip owned by a user. It contains the trip title, optional date range, lifecycle status, and audit timestamps. A trip can contain many route locations and many travel orders.

Main fields:

- `id` - primary key.
- `user_id` - references `USERS.id`.
- `title` - trip name, for example `Vietnam 2026`.
- `start_date`, `end_date` - planned trip dates.
- `status` - trip lifecycle status, such as `PLANNED`, `ACTIVE`, `COMPLETED`, or `CANCELLED`.
- `created_at`, `updated_at` - audit timestamps.

### `TRIP_LOCATIONS`

Stores the ordered route for a trip.

Each row connects a trip to a location and defines when and in what order the traveler visits it. A trip can have many route entries, and the same location can be reused in many trips.

Main fields:

- `id` - primary key.
- `trip_id` - references `TRIPS.id`.
- `location_id` - references `LOCATIONS.id`.
- `visit_order` - position of the location in the trip route.
- `arrival_date`, `departure_date` - planned arrival and departure timestamps.

### `LOCATIONS`

Stores reusable geographic and travel-related places.

Each row represents a place that can be used in routes, orders, or add-on service availability. Locations can be broad places like countries and cities or specific points like airports, hotels, attractions, ports, and stations.

Main fields:

- `id` - primary key.
- `name` - location name.
- `type` - one of the supported location types.
- `country`, `city`, `address` - human-readable address information.
- `latitude`, `longitude` - optional coordinates.

Supported `type` values:

- `COUNTRY`
- `CITY`
- `AIRPORT`
- `TRAIN_STATION`
- `BUS_STATION`
- `PORT`
- `HOTEL`
- `MEETING_POINT`
- `ATTRACTION`
- `OTHER`

### `PROVIDERS`

Stores companies or organizations that provide travel services.

Each row is a provider that can be attached to orders and additional services. Examples include airlines, hotels, booking platforms, tour companies, transport companies, and insurers.

Main fields:

- `id` - primary key.
- `name` - provider name.
- `type` - provider category, such as `AIRLINE`, `HOTEL`, `TOUR_COMPANY`, `TRANSPORT_COMPANY`, `BOOKING_PLATFORM`, `INSURANCE_COMPANY`, or `OTHER`.
- `website` - provider website URL.
- `support_contact` - support phone, email, or other contact details.

### `ADDITIONAL_SERVICES`

Stores optional services that can be recommended or purchased around a trip.

Each row is a service offered by a provider, optionally tied to a location. Examples include airport transfer, insurance, lounge access, eSIM, taxi, extra baggage, tours, and similar add-ons.

Main fields:

- `id` - primary key.
- `title` - service title.
- `description` - details shown to users.
- `service_type` - service category.
- `provider_id` - references `PROVIDERS.id`.
- `location_id` - references `LOCATIONS.id` when the service is location-specific.
- `price_amount`, `price_currency` - optional price.
- `is_active` - whether the service is available for recommendations or ordering.

### `ORDERS`

Stores booked or tracked travel services inside a trip.

Each row belongs to a trip and represents one travel order, booking, reservation, or add-on purchase. It can reference a provider, departure and arrival locations, and uploaded order files.

Main fields:

- `id` - primary key.
- `user_id` - references `USERS.id`.
- `trip_id` - references `TRIPS.id`.
- `provider_id` - references `PROVIDERS.id`.
- `service_type` - service category, for example `FLIGHT`, `TRAIN`, `BUS`, `HOTEL`, `TOUR`, `CAR_RENTAL`, `INSURANCE`, `TAXI`, `ESIM`, `LOUNGE`, `EXTRA_BAGGAGE`, or `OTHER`.
- `external_order_id` - booking code or provider-side order identifier.
- `title` - user-facing order title.
- `status` - current order status.
- `price_amount`, `price_currency` - order price.
- `start_datetime`, `end_datetime` - service start and end times.
- `departure_location_id` - references `LOCATIONS.id`.
- `arrival_location_id` - references `LOCATIONS.id`.
- `created_at`, `updated_at` - audit timestamps.

Supported `status` values:

- `PENDING_VERIFICATION`
- `CONFIRMED`
- `DELAYED`
- `CANCELLED`
- `COMPLETED`
- `REFUND_PENDING`
- `REFUNDED`

### `ORDER_FILES`

Stores files uploaded for an order.

Each row points to one file attached to an order, such as a ticket, boarding pass, invoice, booking confirmation, email export, or structured JSON payload.

Main fields:

- `id` - primary key.
- `order_id` - references `ORDERS.id`.
- `file_url` - URL or storage path for the uploaded file.
- `file_type` - file category, such as `PDF`, `IMAGE`, `EMAIL`, `JSON`, or `OTHER`.
- `parsed_data` - extracted structured content from the file, stored as JSON.
- `uploaded_at` - timestamp when the file was uploaded.

### `REVIEWS`

Stores user reviews.

Each row is a review written by a user for a supported target. The target is polymorphic: `target_type` defines what kind of entity is reviewed, and `target_id` stores the id of that entity.

Main fields:

- `id` - primary key.
- `user_id` - references `USERS.id`.
- `target_type` - reviewed entity type, such as `PROVIDER`, `LOCATION`, `ORDER`, or `ADDITIONAL_SERVICE`.
- `target_id` - id of the reviewed target.
- `rating` - numeric rating, expected to be from 1 to 5.
- `text` - optional review text.
- `created_at`, `updated_at` - audit timestamps.

### `ACHIEVEMENTS`

Stores the achievement catalog.

Each row defines an achievement that users can unlock. The condition fields describe what metric should be checked and what threshold is required.

Main fields:

- `id` - primary key.
- `code` - unique stable code, for example `FIRST_TRIP`.
- `title` - display title.
- `description` - achievement description.
- `condition_type` - metric used to unlock the achievement, such as `TRIPS_COUNT`, `COUNTRIES_COUNT`, `ORDERS_COUNT`, `REVIEWS_COUNT`, or `SPENDING_AMOUNT`.
- `condition_value` - required threshold value.
- `icon_url` - optional icon URL.

### `USER_ACHIEVEMENTS`

Stores achievements unlocked by users.

Each row links one user to one achievement and records when it was unlocked. The same user should not receive the same achievement more than once.

Main fields:

- `id` - primary key.
- `user_id` - references `USERS.id`.
- `achievement_id` - references `ACHIEVEMENTS.id`.
- `unlocked_at` - timestamp when the achievement was unlocked.

## Relationship Summary

- One `USERS` row can have many `TRIPS`, `ORDERS`, `REVIEWS`, `USER_ROLES`, `NOTIFICATION_PREFERENCES`, and `USER_ACHIEVEMENTS`.
- One `TRIPS` row can have many `TRIP_LOCATIONS` and `ORDERS`.
- One `LOCATIONS` row can be reused by many `TRIP_LOCATIONS`, `ORDERS`, and `ADDITIONAL_SERVICES`.
- One `PROVIDERS` row can provide many `ORDERS` and `ADDITIONAL_SERVICES`.
- One `ORDERS` row can have many `ORDER_FILES`.
- One `ACHIEVEMENTS` row can be unlocked by many users through `USER_ACHIEVEMENTS`.
