# GoTrip Backend

Backend-сервис GoTrip для управления поездками, локациями, пользователями, провайдерами, дополнительными услугами, достижениями, уведомлениями и отзывами.

## Стек технологий

- **Scala 3.8.4**
- **sbt 1.12.11**
- **Cats Effect 3.7.0**
- **http4s 0.23.17** + **Tapir 1.13.21**
- **Circe 0.14.15**
- **Skunk 1.0.0** для работы с PostgreSQL
- **Flyway 12.8.1** для миграций
- **PostgreSQL 17** через Docker Compose
- **PureConfig 0.17.10**
- **ScalaTest 3.2.20** + **ScalaMock 7.5.5**
- **Logback** / **log4cats**

## Структура проекта

```text
go-trip/
├── build.sbt
├── docker-compose.yml
├── docs/
│   ├── api/                         # OpenAPI-спецификация
│   ├── database/                    # ER-диаграмма и описание БД
│   └── requirements/                # требования и user stories
└── src/
    ├── main/
    │   ├── resources/
    │   │   ├── application.conf     # настройки сервера и БД
    │   │   ├── logback.xml
    │   │   └── db/migration/        # SQL-миграции Flyway
    │   └── scala/gotrip/
    │       ├── Main.scala           # точка входа, миграции, HTTP-сервер
    │       ├── config/              # загрузка конфигурации
    │       ├── database/            # Flyway + Skunk session pool
    │       ├── domain/              # доменные модели
    │       ├── http/                # Tapir endpoints, controllers, codecs
    │       ├── repository/          # PostgreSQL и in-memory репозитории
    │       └── service/             # бизнес-логика
    └── test/
        └── scala/gotrip/            # ScalaTest specs
```

## Требования

- JDK 21 или новее
- sbt
- Docker и Docker Compose

## Быстрый старт

1. Запустить PostgreSQL:

   ```bash
   docker compose up -d
   ```

2. Запустить приложение:

   ```bash
   sbt run
   ```

3. Открыть Swagger UI:

   ```text
   http://localhost:8080/docs
   ```

При старте приложение загружает конфигурацию, выполняет Flyway-миграции, создает Skunk-пул соединений и поднимает HTTP-сервер.

## Конфигурация

Основные настройки находятся в `src/main/resources/application.conf`.

По умолчанию приложение использует:

```text
host:      0.0.0.0
port:      8080
database:  jdbc:postgresql://localhost:5432/gotrip
user:      gotrip_user
password:  secret
```

Эти значения должны совпадать с параметрами в `docker-compose.yml`.

## Миграции

SQL-миграции лежат в `src/main/resources/db/migration/` и применяются автоматически при запуске приложения.

Формат имени файла:

```text
V{version}__description.sql
```

Например:

```text
V1__initial_schema.sql
```

## Тесты

Запустить все тесты:

```bash
sbt test
```

На текущий момент есть unit-тесты для `LocationService`.

## OpenAPI

Исходная OpenAPI-спецификация находится в `docs/api/gotrip-openapi.yaml`.

Для локального просмотра статической спецификации можно использовать:

```bash
npx swagger-ui-watcher docs/api/gotrip-openapi.yaml
```

Во время работы приложения Swagger UI также доступен из Tapir по адресу:

```text
http://localhost:8080/docs
```

## Диагностика

Если приложение падает на этапе `Running Flyway migrations...` с ошибкой `Connection refused`, значит PostgreSQL недоступен на `localhost:5432`.

Проверьте:

```bash
docker compose ps
docker compose up -d
```

Если команда `sbt` не найдена, установите sbt или добавьте его в `PATH`.
