package gotrip.repository.achievement

import cats.Applicative
import cats.effect.Concurrent
import skunk.Session
import cats.effect.Resource
import gotrip.domain.achievement.{Achievement, AchievementId, AchievementCode}

trait AchievementRepository[F[_]]:
  def findAll(): F[List[Achievement]]
  def findById(id: AchievementId): F[Option[Achievement]]
  def findByCode(code: AchievementCode): F[Option[Achievement]]
  def create(achievement: Achievement): F[Achievement]
  def update(achievement: Achievement): F[Int]
  def delete(id: AchievementId): F[Int]

object AchievementRepository:
  def makeInMemory[F[_]: Applicative]: F[AchievementRepository[F]] =
    InMemoryAchievementRepository.make

  def makePostgres[F[_]: Concurrent](
    sessionPool: Resource[F, Session[F]]
  ): AchievementRepository[F] =
    PostgresAchievementRepository.make(sessionPool)
