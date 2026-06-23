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
├── docker-compose.yml               # PostgreSQL контейнер
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

## Быстрый старт

1. **Запустить PostgreSQL**
   ```bash
   docker-compose up -d
   ```

2. **Запустить приложение**
   ```bash
   sbt run
   ```

## Конфигурация

Файл `application.conf` содержит параметры подключения к БД (url, user, password, driver, пул соединений).  
При необходимости измените их под своё окружение.

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

Пока приложение запускает миграции, инициализирует Skunk-пул соединений и завершает работу, потому что HTTP-сервер ещё не добавлен.
