# GoTrip Backend

Сервис для управления поездками и заказами.

## Документация
- [UserStories](https://docs.google.com/document/d/12_VgpVLQ9lQUd1dlmlBCyuZuOkQP9pXCvZ2K7SpG6-w/edit?tab=t.0)
- [Specification](https://docs.google.com/document/d/1Q58Y4Cjxm6CMPYLfaArhQhrOVFkvLBKbki20aSrJ63M/edit?tab=t.0#heading=h.6tsam0yuh976)

## Стек технологий

- **Scala 3.8.4** — язык программирования
- **Cats Effect 3.7.0** — функциональные эффекты
- **Doobie 1.0.0-RC12** — работа с БД (JDBC + функциональный слой)
- **Flyway 12.8.1** — миграции схемы БД
- **PostgreSQL 17** — база данных (запускается через Docker Compose)
- **PureConfig 0.17.10** — типобезопасная конфигурация
- **Logback** — логирование

## Структура проекта

```
go-trip/
├── docker-compose.yml          # PostgreSQL контейнер
├── build.sbt                   # сборка и зависимости
├── src/
│   ├── main/
│   │   ├── resources/
│   │   │   ├── application.conf        # конфигурация приложения
│   │   │   ├── logback.xml             # настройки логгера
│   │   │   └── db/migration/           # SQL-скрипты Flyway
│   │   └── scala/gotrip/
│   │       ├── Main.scala               # точка входа
│   │       ├── config/                  # загрузка конфигурации
│   │       ├── database/                # DbTransactor и миграции
│   │       ├── domain/                  # case class модели
│   │       └── repository/              # репозитории (CRUD)
```

## Подготовка окружения

### 1. Запуск PostgreSQL через Docker Compose

В корне проекта выполните:

```bash
docker-compose up -d
```

PostgreSQL будет доступен на порту `5432` с параметрами:

- База данных: `gotrip`
- Пользователь: `gotrip_user`
- Пароль: `secret`

### 2. Конфигурация приложения

Файл `src/main/resources/application.conf` уже настроен для подключения к этой БД. При необходимости вы можете изменить параметры (url, user, password).

### 3. Миграции Flyway

Миграции запускаются автоматически при старте приложения. SQL-скрипты лежат в `src/main/resources/db/migration/`.  
Первый скрипт `V1__init.sql` создаёт все необходимые таблицы.

## Запуск приложения

```bash
sbt run
```

## Работа с БД через Doobie

Все запросы к БД выполняются в эффекте `ConnectionIO` и транзактируются через `Transactor`.  
Пример репозитория (`UserRepository`) уже включает методы `create`, `findByEmail`, `addRole` и т.д.

Модели данных (case class) находятся в пакете `gotrip.domain`. Для каждой модели определён `Read` для извлечения из БД, а для некоторых и `Write`.

## Доступные эндпоинты (в разработке)

На данный момент реализован только модуль подключения к БД и миграции. REST API будет добавлен позже с использованием http4s или Tapir в соответствии с OpenAPI-спецификацией.