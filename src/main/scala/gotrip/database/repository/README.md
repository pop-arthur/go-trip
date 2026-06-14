# Repository Layer

Репозитории предоставляют интерфейс для доступа к базе данных с использованием Doobie.

## Принцип работы

Каждый репозиторий:

- Принимает `Transactor[F[_]]` (обычно `Transactor[IO]`) через фабричный метод `make`.
- Возвращает эффект `F[T]`, где `F` — любой тип с `MonadCancelThrow` (например, `IO`).
- Все SQL-запросы пишутся внутри интерполятора `sql` и преобразуются в `ConnectionIO[T]`, после чего транзактируются через `.transact(xa)`.

## Пример

```scala
trait UserRepository[F[_]] {
  def findByEmail(email: String): F[Option[User]]
}

object UserRepository {
  def make[F[_]: MonadCancelThrow](xa: Transactor[F]): UserRepository[F] =
    new UserRepository[F] {
      def findByEmail(email: String): F[Option[User]] =
        sql"SELECT * FROM users WHERE email = $email".query[User].option.transact(xa)
    }
}
```

## Текущие репозитории

- `UserRepository` — CRUD пользователей, добавление ролей.
- (Остальные будут добавлены по мере необходимости)