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
├── docker-compose.yml               # PostgreSQL + приложение
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

## Быстрый старт через Docker

1. Создать локальный `.env`:

   ```bash
   cp .env.example .env
   ```

2. Собрать Docker-образ приложения:

   ```bash
   sbt native
   ```

   Проверить:

   ```bash
   docker images | grep gotrip-backend
   ```

3. Поднять приложение и PostgreSQL:

   ```bash
   docker compose up
   ```

4. Проверить API:

   ```text
   http://localhost:8080/docs
   ```

5. Остановить контейнеры:

   ```bash
   docker compose down
   ```

   Чтобы также удалить volume с данными PostgreSQL:

   ```bash
   docker compose down -v
   ```

При старте приложение загружает конфигурацию, выполняет Flyway-миграции, создает Skunk-пул соединений и поднимает HTTP-сервер.

## Конфигурация

Основные настройки находятся в `src/main/resources/application.conf`.

Файл содержит локальные значения по умолчанию и поддерживает переопределение через переменные окружения. Пароли и секреты дефолтных значений не имеют.

Для локального запуска вне Docker по умолчанию используется `localhost:5432`, внутри Docker Compose — сервис `postgres`.

В контейнере приложения настройки переопределяются переменными окружения:

- `GOTRIP_DB_URL=jdbc:postgresql://postgres:5432/gotrip`
- `GOTRIP_DB_HOST=postgres`
- `GOTRIP_DB_PORT=5432`
- `GOTRIP_DB_NAME=gotrip`
- `GOTRIP_DB_USER=gotrip_user`
- `GOTRIP_DB_PASSWORD` — пароль БД, задаётся через `.env` или окружение
- `GOTRIP_SERVER_HOST=0.0.0.0`
- `GOTRIP_SERVER_PORT=8080`

Если после смены пароля PostgreSQL падает с `password authentication failed`, а volume уже существовал, пересоздайте локальный volume:

```bash
docker compose down -v
docker compose up
```

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
