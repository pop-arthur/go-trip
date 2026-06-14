# Configuration

Загрузка конфигурации из `application.conf` с помощью PureConfig.

## Файлы

- `DatabaseConfig.scala` — case class, описывающий параметры подключения к БД (url, user, password, driver, connectionPool).

## Пример использования

```scala
val config = ConfigSource.default.at("gotrip.database").load[DatabaseConfig]
```

Файл конфигурации должен быть в `src/main/resources/application.conf` в формате HOCON.