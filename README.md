# GoTrip Backend

Сервис для управления поездками и заказами (базовая инфраструктура).

## Стек технологий

- **Scala 3.8.4**
- **Cats Effect 3.7.0**
- **Skunk 1.0.0** (типобезопасная работа с PostgreSQL)
- **Flyway 12.8.1** (миграции)
- **PostgreSQL 17** (контейнер Docker)
- **PureConfig 0.17.10** (конфигурация)
- **Logback** (логирование)

## Структура проекта (текущая)

```
go-trip/
├── docker-compose.yml               # PostgreSQL + приложение
├── build.sbt
├── README.md
└── src/
    ├── main/
    │   ├── resources/
    │   │   ├── application.conf     # настройки БД
    │   │   ├── logback.xml
    │   │   └── db/migration/        # SQL-скрипты Flyway
    │   └── scala/gotrip/
    │       ├── Main.scala           # точка входа
    │       ├── config/              # загрузка конфигурации
    │       ├── database/            # SkunkSessionPool + Flyway
    │       ├── domain/              # доменные модели
    │       ├── repository/          # доступ к данным
    │       └── service/             # бизнес-логика
```

## Быстрый старт через Docker

Приложение запускается из Docker-образа, собранного через `sbt-native-packager`.

1. **Собрать Docker-образ приложения**
   ```bash
   sbt native
   ```

   Проверить:
   ```bash
   docker images | grep gotrip-backend
   ```

   После сборки локально появится образ:
   ```bash
   gotrip-backend:0.1.0-SNAPSHOT
   ```

2. **Поднять приложение и PostgreSQL**
   ```bash
   docker compose up
   ```

3. **Проверить API**
   - приложение: `http://localhost:8080`
   - Swagger UI: `http://localhost:8080/docs`

4. **Остановить контейнеры**
   ```bash
   docker compose down
   ```

   Чтобы также удалить volume с данными PostgreSQL:
   ```bash
   docker compose down -v
   ```

## Docker-конфигурация

`docker-compose.yml` поднимает два сервиса:
- `postgres` — PostgreSQL 17 с healthcheck;
- `app` — образ `gotrip-backend:0.1.0-SNAPSHOT`, собранный через `sbt native`.

В контейнере приложения настройки переопределяются переменными окружения:
- `GOTRIP_DB_URL=jdbc:postgresql://postgres:5432/gotrip`
- `GOTRIP_DB_HOST=postgres`
- `GOTRIP_DB_PORT=5432`
- `GOTRIP_DB_NAME=gotrip`
- `GOTRIP_DB_USER=gotrip_user`
- `GOTRIP_DB_PASSWORD` — пароль БД, задаётся через `.env` или окружение
- `GOTRIP_SERVER_HOST=0.0.0.0`
- `GOTRIP_SERVER_PORT=8080`

## Конфигурация

Файл `application.conf` содержит локальные значения по умолчанию и поддерживает переопределение через переменные окружения. Пароли и секреты дефолтных значений не имеют.
Для локального запуска вне Docker по умолчанию используется `localhost:5432`, внутри Docker Compose — сервис `postgres`.

Для локальной разработки используйте `.env`:

```bash
cp .env.example .env
```

## Миграции

SQL-скрипты лежат в `src/main/resources/db/migration/` и применяются автоматически при старте.  
Именование: `V{версия}__описание.sql`.  

## Добавление бизнес-логики

Уже заложены:
- domain-модели для локаций
- репозитории для локаций: in-memory и PostgreSQL через Skunk
- сервисный слой для поиска и получения локаций

В будущем здесь появятся:
- HTTP API (http4s / Tapir)
- тесты (TestContainers)

Приложение запускает миграции, инициализирует Skunk-пул соединений и стартует HTTP-сервер.
