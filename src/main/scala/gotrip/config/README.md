# Configuration

Загрузка параметров подключения к базе данных из `application.conf` с помощью PureConfig.

## Файл

- `DatabaseConfig.scala` – case class, десериализующий секцию `gotrip.database` конфига.

## Пример `application.conf`

```hocon
gotrip {
  database {
    url = "jdbc:postgresql://localhost:5432/gotrip"
    user = "gotrip_user"
    password = "secret"
    driver = "org.postgresql.Driver"
    connectionPool {
      maxSize = 10
      minimumIdle = 2
    }
  }
}
```

## Загрузка в коде

```scala
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException

val config = ConfigSource.default.at("gotrip.database").load[DatabaseConfig]
  .leftMap(e => ConfigReaderException[DatabaseConfig](e))
```
