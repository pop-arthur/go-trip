# Database module

Модуль обеспечивает безопасное подключение к PostgreSQL и запуск миграций Flyway.

## Содержание

- `DbTransactor.scala` – создание `Resource[F, HikariTransactor[F]]` (пул соединений HikariCP).
- `Migration.scala` – применение миграций Flyway при старте приложения.

## Использование

В `Main.scala`:

```scala
for {
  config <- loadDatabaseConfig
  _ <- DbTransactor[IO](config).use { xa =>
    Migration.migrate[IO](config) >>
    // здесь будет бизнес-логика, использующая xa
  }
} yield ()
```

## Миграции

Файлы миграций находятся в `resources/db/migration/`.