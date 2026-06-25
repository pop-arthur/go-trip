package gotrip.repository.userachievement

import cats.Applicative
import cats.effect.Concurrent
import skunk.Session
import cats.effect.Resource
import gotrip.domain.user.UserId
import gotrip.domain.achievement.AchievementId
import gotrip.domain.userachievement.UserAchievement

trait UserAchievementRepository[F[_]]:
  def create(userId: UserId, achievementId: AchievementId): F[UserAchievement]
  def findByUserId(userId: UserId): F[List[UserAchievement]]
  def findByAchievementId(achievementId: AchievementId): F[List[UserAchievement]]
  def delete(userId: UserId, achievementId: AchievementId): F[Int]

object UserAchievementRepository:
  def makeInMemory[F[_]: Applicative]: F[UserAchievementRepository[F]] =
    InMemoryUserAchievementRepository.make

  def makePostgres[F[_]: Concurrent](
    sessionPool: Resource[F, Session[F]]
  ): UserAchievementRepository[F] =
    PostgresUserAchievementRepository.make(sessionPool)