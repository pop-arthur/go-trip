# GoTrip

GoTrip - backend-сервис для travel assistant MVP с REST API, PostgreSQL-хранилищем и простым статическим web-интерфейсом. Проект покрывает базовый пользовательский сценарий планирования поездки: регистрация, авторизация, маршруты, заказы, файлы заказов, провайдеры, дополнительные услуги, рекомендации, отзывы, уведомления и достижения.

## Возможности

- Регистрация, вход, refresh/logout и JWT-аутентификация.
- Роли пользователей `USER` и `ADMIN`.
- Управление профилем текущего пользователя.
- CRUD для поездок и точек маршрута.
- Работа с заказами внутри поездки, статусами заказов и файлами заказов.
- Каталоги локаций, провайдеров и дополнительных услуг.
- Пользовательские уведомления и настройки уведомлений.
- Отзывы, агрегированная оценка и достижения.
- Рекомендации дополнительных услуг по поездке или заказу.
- Admin endpoints для управления провайдерами, услугами, достижениями и симуляции смены статуса заказа.
- Swagger UI, собранный из Tapir endpoints.
- Seed-данные для локальной разработки и демонстрации.

## Технологии

- Scala `3.8.4`
- sbt `1.12.11`
- Cats Effect `3.7.0`
- http4s `0.23.17`
- Tapir `1.13.21`
- Circe `0.14.15`
- Skunk `1.0.0`
- Flyway `12.8.1`
- PostgreSQL `17`
- PureConfig `0.17.10`
- ScalaTest, ScalaMock, munit-cats-effect, Testcontainers
- Logback и log4cats
- Docker, Docker Compose

## Структура проекта

```text
go-trip/
├── build.sbt                         # зависимости, настройки sbt и Docker packaging
├── Dockerfile                        # multi-stage образ backend-приложения
├── docker-compose.yml                # backend, PostgreSQL и frontend-сервисы
├── .env.example                      # пример локальных переменных окружения
├── frontend/                         # статический HTML/CSS/JS интерфейс
├── frontend-react/                   # основной React-интерфейс
├── docs/
│   ├── api/docs.yaml                 # статическая OpenAPI 3.1 спецификация
│   ├── database/                     # ER-диаграмма и описание схемы БД
│   └── requirements/                 # PDF со спецификацией и user stories
└── src/
    ├── main/
    │   ├── resources/
    │   │   ├── application.conf      # конфигурация сервера, БД и auth
    │   │   ├── logback.xml
    │   │   └── db/migration/         # Flyway SQL-миграции и seed-данные
    │   └── scala/gotrip/
    │       ├── Main.scala            # wiring приложения и запуск HTTP server
    │       ├── config/               # PureConfig-модели
    │       ├── database/             # Flyway и Skunk session pool
    │       ├── domain/               # доменные модели
    │       ├── http/                 # Tapir endpoints, controllers, codecs
    │       ├── repository/           # PostgreSQL и in-memory репозитории
    │       └── service/              # бизнес-логика
    └── test/scala/gotrip/            # service и repository specs
```

## Требования

- JDK 21 или новее
- sbt
- Docker и Docker Compose

Docker нужен не только для запуска через Compose, но и для интеграционных тестов репозиториев: они поднимают PostgreSQL через Testcontainers.

## Быстрый старт

Создайте локальный файл окружения:

```bash
cp .env.example .env
```

Минимально нужен `GOTRIP_DB_PASSWORD`. В `.env.example` также есть admin-настройки:

```env
GOTRIP_DB_PASSWORD=replace-me
GOTRIP_ADMIN_EMAIL=admin@example.com
GOTRIP_ADMIN_PASSWORD=replace-me-admin-password
GOTRIP_ADMIN_FULL_NAME=GoTrip Admin
```

Соберите и запустите backend, PostgreSQL и оба frontend-интерфейса:

```bash
docker compose up --build
```

После запуска доступны:

```text
http://localhost:8080/docs   # Swagger UI
http://localhost:3000        # React-интерфейс
http://localhost:8081        # статический web-интерфейс
```

Остановить контейнеры:

```bash
docker compose down
```

Остановить контейнеры и удалить volume PostgreSQL:

```bash
docker compose down -v
```

При старте backend загружает конфигурацию, выполняет Flyway-миграции, создает Skunk session pool, собирает Tapir routes и запускает http4s server.

## Frontend

В репозитории есть два frontend-интерфейса:

- `frontend-react/` — основной React 18 интерфейс. В Docker Compose он работает в dev-режиме на `http://localhost:3000`, а API-запросы проксируются на backend-контейнер `app:8080`.
- `frontend/` — простой статический HTML/CSS/JS интерфейс. Nginx отдает его на `http://localhost:8081`; интерфейс обращается к API на `http://localhost:8080`.

Запустить только React-интерфейс вместе с backend и базой:

```bash
docker compose up --build postgres app frontend-react
```

Запустить только статический интерфейс вместе с backend и базой:

```bash
docker compose up --build postgres app frontend
```

Для локальной разработки React без frontend-контейнера сначала запустите PostgreSQL и backend, затем dev-сервер:

```bash
docker compose up -d postgres app
cd frontend-react
npm ci
REACT_APP_API_URL=http://localhost:8080 npm start
```

Для production-сборки React:

```bash
cd frontend-react
npm ci
npm run build
```

## Demo-данные

Миграция `V4__seed_default_data.sql` создает тестовых пользователей:

```text
demo.user1@example.com / Password123
demo.user2@example.com / Password123
demo.user3@example.com / Password123
demo.user4@example.com / Password123
demo.user5@example.com / Password123
```

Если в `.env` заданы `GOTRIP_ADMIN_EMAIL` и `GOTRIP_ADMIN_PASSWORD`, миграция `V3__seed_admin_user.sql` создает или обновляет администратора с ролями `USER` и `ADMIN`.

## Локальный запуск backend

Можно запустить только PostgreSQL из Compose:

```bash
docker compose up -d postgres
```

Затем запустить приложение из sbt:

```bash
export GOTRIP_DB_PASSWORD=replace-me
sbt run
```

По умолчанию backend слушает `0.0.0.0:8080` и подключается к базе `gotrip` на `localhost:5432` пользователем `gotrip_user`.

## Конфигурация

Базовые значения находятся в `src/main/resources/application.conf`. Их можно переопределять переменными окружения:

| Variable | Description | Default |
|---|---|---|
| `GOTRIP_SERVER_HOST` | host HTTP-сервера | `0.0.0.0` |
| `GOTRIP_SERVER_PORT` | port HTTP-сервера | `8080` |
| `GOTRIP_DB_URL` | JDBC URL для Flyway | `jdbc:postgresql://localhost:5432/gotrip` |
| `GOTRIP_DB_HOST` | host PostgreSQL для Skunk | `localhost` |
| `GOTRIP_DB_PORT` | port PostgreSQL для Skunk | `5432` |
| `GOTRIP_DB_NAME` | имя базы данных | `gotrip` |
| `GOTRIP_DB_USER` | пользователь БД | `gotrip_user` |
| `GOTRIP_DB_PASSWORD` | пароль БД | обязательна |
| `GOTRIP_DB_POOL_MAX_SIZE` | максимальный размер пула | `10` |
| `GOTRIP_DB_POOL_MIN_IDLE` | минимальное число idle-сессий | `2` |
| `GOTRIP_ADMIN_EMAIL` | email admin-пользователя для seed-миграции | пусто |
| `GOTRIP_ADMIN_PASSWORD` | пароль admin-пользователя для seed-миграции | пусто |
| `GOTRIP_ADMIN_FULL_NAME` | имя admin-пользователя | `GoTrip Admin` |

`GOTRIP_DB_PASSWORD` обязателен: дефолтного пароля в конфиге нет.

JWT-secret сейчас задан dev-значением в `application.conf`. Для production его нужно вынести в секреты и заменить.

## API

Swagger UI доступен при запущенном backend:

```text
http://localhost:8080/docs
```

Статическая OpenAPI 3.1 спецификация лежит в:

```text
docs/api/docs.yaml
```

Основные группы API:

- `Auth`: регистрация, вход, refresh, logout.
- `Users`: профиль текущего пользователя.
- `Trips`: поездки.
- `Trip locations`: маршрут поездки.
- `Orders`: заказы, статусы заказов и admin-симуляция смены статуса.
- `Order files`: файлы и parsed data по заказам.
- `Locations`: локации.
- `Providers`: провайдеры и admin-управление провайдерами.
- `Additional services`: дополнительные услуги и admin-управление услугами.
- `Notifications`: уведомления пользователя.
- `Notification preferences`: настройки уведомлений.
- `Achievements`: каталог достижений и admin-управление.
- `User achievements`: достижения текущего пользователя.
- `Reviews`: отзывы и rating summary.
- `Recommendations`: рекомендации по поездке или заказу.

Для просмотра статической спецификации можно использовать любой OpenAPI/Swagger viewer, например:

```bash
npx swagger-ui-watcher docs/api/docs.yaml
```

## База данных

Миграции лежат в:

```text
src/main/resources/db/migration/
```

Они применяются автоматически при запуске backend.

Текущие миграции:

- `V1__initial_schema.sql` - основная схема: пользователи, роли, поездки, маршруты, заказы, файлы, провайдеры, услуги, отзывы, достижения, уведомления.
- `V2__auth_sessions.sql` - refresh-сессии для auth flow.
- `V3__seed_admin_user.sql` - создание или обновление admin-пользователя из env.
- `V4__seed_default_data.sql` - demo-данные для локальной разработки.

Документация по схеме и ER-диаграмма:

```text
docs/database/README.md
docs/database/er-diagram.png
docs/database/er-diagram.drawio
```

## Тесты

Запуск всех тестов:

```bash
sbt test
```

В проекте есть:

- unit-тесты сервисов в `src/test/scala/gotrip/service`;
- интеграционные тесты PostgreSQL-репозиториев в `src/test/scala/gotrip/repository`.

Repository specs используют Testcontainers, поэтому перед запуском должен быть доступен Docker.

## Полезные команды

| Command | Description |
|---|---|
| `sbt run` | запустить backend локально |
| `sbt test` | запустить тесты |
| `sbt native` | собрать локальный Docker-образ `gotrip-backend` через sbt-native-packager |
| `docker compose up --build` | собрать и поднять backend, PostgreSQL и оба frontend-интерфейса |
| `docker compose up -d postgres` | поднять только PostgreSQL |
| `docker compose down` | остановить контейнеры |
| `docker compose down -v` | остановить контейнеры и удалить volume БД |

## Диагностика

`GOTRIP_DB_PASSWORD is required`  
Создайте `.env` из `.env.example` и задайте `GOTRIP_DB_PASSWORD`.

`Connection refused` во время `Running Flyway migrations...`  
PostgreSQL недоступен. Проверьте `docker compose ps`, порт `5432` и значения `GOTRIP_DB_*`.

`password authentication failed`  
Пароль в `.env` не совпадает с паролем в уже созданном PostgreSQL volume. Для локальной разработки проще пересоздать volume:

```bash
docker compose down -v
docker compose up
```

`sbt: command not found`  
Установите sbt или добавьте его в `PATH`.

Testcontainers-тесты не стартуют  
Проверьте, что Docker запущен и доступен текущему пользователю.

## Документация

- `docs/api/docs.yaml` - OpenAPI-спецификация.
- `docs/database/README.md` - описание таблиц и связей.
- `docs/database/er-diagram.png` - ER-диаграмма.
- `docs/requirements/` - спецификация и user stories в PDF.
