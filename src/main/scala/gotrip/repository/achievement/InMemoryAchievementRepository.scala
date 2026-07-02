package gotrip.repository.achievement

import cats.Applicative
import cats.syntax.all._
import gotrip.domain.achievement._
import scala.collection.mutable

object InMemoryAchievementRepository {
  def make[F[_]: Applicative]: F[AchievementRepository[F]] = {
    val store = mutable.Map.empty[AchievementId, Achievement]

    new AchievementRepository[F] {
      override def findAll(): F[List[Achievement]] = store.values.toList.pure[F]

      override def findById(id: AchievementId): F[Option[Achievement]] = store.get(id).pure[F]

      override def findByCode(code: AchievementCode): F[Option[Achievement]] =
        store.values.find(_.code == code).pure[F]

      override def create(achievement: Achievement): F[Achievement] = {
        store += (achievement.id -> achievement)
        achievement.pure[F]
      }

      override def update(achievement: Achievement): F[Int] = {
        store.get(achievement.id) match {
          case Some(_) =>
            store += (achievement.id -> achievement)
            1.pure[F]
          case None => 0.pure[F]
        }
      }

      override def delete(id: AchievementId): F[Int] =
        store.remove(id).map(_ => 1).getOrElse(0).pure[F]
    }.pure[F]
  }
}
