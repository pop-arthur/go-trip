package gotrip.service.achievement

import cats.effect.{Clock, Sync}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import gotrip.domain.achievement.{Achievement, AchievementId, AchievementCode}
import gotrip.repository.achievement.AchievementRepository
import gotrip.service.GeneratedData

final class AchievementService[F[_]: Sync: Clock](
  repo: AchievementRepository[F]
):

  def listAll(): F[List[Achievement]] = repo.findAll()
  def findById(id: AchievementId): F[Option[Achievement]] = repo.findById(id)
  def findByCode(code: AchievementCode): F[Option[Achievement]] = repo.findByCode(code)

  def create(achievement: Achievement): F[Achievement] =
    for
      id <- GeneratedData.newId[F]
      now <- GeneratedData.now[F]
      created <- repo.create(achievement.copy(id = AchievementId(id), createdAt = now, updatedAt = now))
    yield created

  def update(achievement: Achievement): F[Int] =
    GeneratedData.now[F].flatMap(now => repo.update(achievement.copy(updatedAt = now)))

  def delete(id: AchievementId): F[Int] = repo.delete(id)
