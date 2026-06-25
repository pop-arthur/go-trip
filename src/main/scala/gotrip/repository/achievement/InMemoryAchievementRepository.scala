package gotrip.repository.achievement

import cats.Applicative
import cats.syntax.all._
import gotrip.domain.achievement._
import java.time.Instant
import scala.collection.mutable

object InMemoryAchievementRepository {
  def make[F[_]: Applicative]: F[AchievementRepository[F]] = {
    val store = mutable.Map.empty[AchievementId, Achievement]
    var nextId = 1L

    def newId(): AchievementId = { val id = nextId; nextId += 1; AchievementId(id) }

    new AchievementRepository[F] {
      override def findAll(): F[List[Achievement]] = store.values.toList.pure[F]

      override def findById(id: AchievementId): F[Option[Achievement]] = store.get(id).pure[F]

      override def findByCode(code: AchievementCode): F[Option[Achievement]] =
        store.values.find(_.code == code).pure[F]

      override def create(achievement: Achievement): F[Achievement] = {
        val now = Instant.now()
        val newAchievement = achievement.copy(
          id = newId(),
          createdAt = now,
          updatedAt = now
        )
        store += (newAchievement.id -> newAchievement)
        newAchievement.pure[F]
      }

      override def update(achievement: Achievement): F[Int] = {
        store.get(achievement.id) match {
          case Some(_) =>
            val updated = achievement.copy(updatedAt = Instant.now())
            store += (updated.id -> updated)
            1.pure[F]
          case None => 0.pure[F]
        }
      }

      override def delete(id: AchievementId): F[Int] =
        store.remove(id).map(_ => 1).getOrElse(0).pure[F]
    }.pure[F]
  }
}