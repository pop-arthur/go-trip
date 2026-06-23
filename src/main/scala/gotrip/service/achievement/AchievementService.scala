package gotrip.service.achievement

import gotrip.domain.achievement.{Achievement, AchievementId, AchievementCode}
import gotrip.repository.achievement.AchievementRepository

final class AchievementService[F[_]](
  repo: AchievementRepository[F]
):

  def listAll(): F[List[Achievement]] = repo.findAll()
  def findById(id: AchievementId): F[Option[Achievement]] = repo.findById(id)
  def findByCode(code: AchievementCode): F[Option[Achievement]] = repo.findByCode(code)

  def create(achievement: Achievement): F[Achievement] = repo.create(achievement)
  def update(achievement: Achievement): F[Int] = repo.update(achievement)
  def delete(id: AchievementId): F[Int] = repo.delete(id)