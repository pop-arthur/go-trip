# Database module

Модуль обеспечивает безопасное подключение к PostgreSQL и запуск миграций Flyway.

## Содержание

- `SkunkSessionPool.scala` – создание пула `Session[F]` для работы с PostgreSQL через Skunk.
- `Migration.scala` – применение миграций Flyway при старте приложения.

## Использование

В `Main.scala`:

```scala
for {
  config <- loadDatabaseConfig
  _ <- Migration.migrate[IO](config)
  _ <- SkunkSessionPool[IO](config).use { sessionPool =>
    // здесь будет бизнес-логика, использующая sessionPool
  }
} yield ()
```

## Миграции

Файлы миграций находятся в `resources/db/migration/`.
