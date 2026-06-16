# GoTrip Backend

Сервис для управления поездками и заказами (базовая инфраструктура).

## Стек технологий

- **Scala 3.8.4**
- **Cats Effect 3.7.0**
- **Doobie 1.0.0-RC12** (работа с PostgreSQL)
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
    │       └── database/            # DbTransactor + Flyway
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

В будущем здесь появятся:
- domain-модели (case class)
- репозитории (Doobie + Transactor)
- HTTP API (http4s / Tapir)
- тесты (TestContainers)

Пока реализована только инфраструктура подключения к БД и миграции.