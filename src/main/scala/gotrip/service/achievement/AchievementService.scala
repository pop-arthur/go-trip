package gotrip.service.achievement

import cats.effect.{Clock, Sync}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import gotrip.domain.achievement.{Achievement, AchievementId, AchievementCode}
import gotrip.repository.achievement.AchievementRepository
import gotrip.service.GeneratedData

trait AchievementService[F[_]] {
  def listAll(): F[List[Achievement]]
  def findById(id: AchievementId): F[Option[Achievement]]
  def findByCode(code: AchievementCode): F[Option[Achievement]]
  def create(achievement: Achievement): F[Achievement]
  def update(achievement: Achievement): F[Int]
  def delete(id: AchievementId): F[Int]
}

object AchievementService {
  def make[F[_]: Sync: Clock: GeneratedData](repo: AchievementRepository[F]): AchievementService[F] =
    new AchievementService[F] {
      override def listAll(): F[List[Achievement]] = repo.findAll()
      override def findById(id: AchievementId): F[Option[Achievement]] = repo.findById(id)
      override def findByCode(code: AchievementCode): F[Option[Achievement]] = repo.findByCode(code)
      override def create(achievement: Achievement): F[Achievement] =
        for {
          id <- GeneratedData[F].newId()
          now <- GeneratedData[F].now()
          created <- repo.create(achievement.copy(id = AchievementId(id), createdAt = now, updatedAt = now))
        } yield created
      override def update(achievement: Achievement): F[Int] =
        GeneratedData[F].now().flatMap(now => repo.update(achievement.copy(updatedAt = now)))
      override def delete(id: AchievementId): F[Int] = repo.delete(id)
    }
}