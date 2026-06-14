# Database Module

Модуль отвечает за создание пула соединений с PostgreSQL и запуск миграций Flyway.

## Файлы

- `DbTransactor.scala` — создание `Resource[F, HikariTransactor[F]]` с настройками пула (HikariCP).
- `Migration.scala` — запуск миграций Flyway при старте приложения.

## Использование

В `Main.scala`:

```scala
for {
  config <- loadConfig
  _ <- DbTransactor[IO](config).use { xa =>
    for {
      _ <- Migration.migrate[IO](config)
      // создание репозиториев с xa
    } yield ()
  }
} yield ()
```

## Миграции

SQL-скрипты лежат в `src/main/resources/db/migration/` и именуются по шаблону `V{version}__description.sql`.  
Первая миграция — `V1__init.sql`, создаёт все таблицы (см. схему в `gotrip-openapi.yaml` и ER-диаграмму).

