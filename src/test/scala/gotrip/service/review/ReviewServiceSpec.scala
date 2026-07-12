package gotrip.service.review

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import gotrip.domain.review._
import gotrip.domain.user.UserId
import gotrip.repository.review.ReviewRepository
import gotrip.service.{GeneratedData, GeneratedDataTestSupport}
import gotrip.service.achievement.AchievementEngine
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.Instant
import java.util.UUID

final class ReviewServiceSpec extends AnyWordSpec with Matchers with MockFactory with GeneratedDataTestSupport {

  "ReviewService" should {
    "create a review" in {
      val repo = mock[ReviewRepository[IO]]
      val generatedData = generatedDataMock
      val service = serviceWith(repo, generatedData)

      expectGeneratedId(generatedData, reviewId.value)
      expectGeneratedNow(generatedData, generatedAt)
      repo.create.expects(review).returning(IO.pure(review))

      service.create(review).unsafeRunSync() shouldBe review
    }

    "find review by id" in {
      val repo = mock[ReviewRepository[IO]]
      val service = serviceWithDefault(repo)

      repo.findById.expects(reviewId).returning(IO.pure(Some(review)))

      service.findById(reviewId).unsafeRunSync() shouldBe Some(review)
    }

    "find reviews by target" in {
      val repo = mock[ReviewRepository[IO]]
      val service = serviceWithDefault(repo)

      repo.findByTarget.expects(targetType, targetId).returning(IO.pure(List(review)))

      service.findByTarget(targetType, targetId).unsafeRunSync() shouldBe List(review)
    }

    "find reviews by user" in {
      val repo = mock[ReviewRepository[IO]]
      val service = serviceWithDefault(repo)

      repo.findByUserId.expects(userId).returning(IO.pure(List(review)))

      service.findByUser(userId).unsafeRunSync() shouldBe List(review)
    }

    "update a review" in {
      val repo = mock[ReviewRepository[IO]]
      val generatedData = generatedDataMock
      val service = serviceWith(repo, generatedData)

      val updated = review.copy(rating = ReviewRating(4))
      expectGeneratedNow(generatedData, updatedAt)
      repo.update.expects(updated.copy(updatedAt = updatedAt)).returning(IO.pure(1))

      service.update(updated).unsafeRunSync() shouldBe 1
    }

    "delete a review" in {
      val repo = mock[ReviewRepository[IO]]
      val service = serviceWithDefault(repo)

      repo.delete.expects(reviewId).returning(IO.pure(1))

      service.delete(reviewId).unsafeRunSync() shouldBe 1
    }

    "calculate average rating" in {
      val repo = mock[ReviewRepository[IO]]
      val service = serviceWithDefault(repo)

      repo.averageRating.expects(targetType, targetId).returning(IO.pure(Some(4.5)))

      service.averageRating(targetType, targetId).unsafeRunSync() shouldBe Some(4.5)
    }

    "return None average rating when no reviews exist" in {
      val repo = mock[ReviewRepository[IO]]
      val service = serviceWithDefault(repo)

      repo.averageRating.expects(targetType, targetId).returning(IO.pure(None))

      service.averageRating(targetType, targetId).unsafeRunSync() shouldBe None
    }
  }

  private def uuid(suffix: String): UUID =
    UUID.fromString(s"00000000-0000-0000-0000-$suffix")

  private def serviceWith(
    repository: ReviewRepository[IO],
    generatedData: GeneratedData[IO]
  ): ReviewService[IO] =
    given GeneratedData[IO] = generatedData
    val achievementEngine = mock[AchievementEngine[IO]]
    achievementEngine.checkAndUnlock.expects(*, *).returning(IO.unit).anyNumberOfTimes()
    new ReviewService[IO](repository, achievementEngine)

  private def serviceWithDefault(repository: ReviewRepository[IO]): ReviewService[IO] =
    val achievementEngine = mock[AchievementEngine[IO]]
    achievementEngine.checkAndUnlock.expects(*, *).returning(IO.unit).anyNumberOfTimes()
    new ReviewService[IO](repository, achievementEngine)

  private val userId = UserId(uuid("000000000001"))
  private val reviewId = ReviewId(uuid("000000000100"))
  private val generatedAt = Instant.parse("2026-06-01T10:00:00Z")
  private val updatedAt = Instant.parse("2026-06-01T10:05:00Z")
  private val targetType = ReviewTargetType.Provider
  private val targetId = ReviewTargetId(uuid("000000000042"))
  private val rating = ReviewRating(5)
  private val text = ReviewText(Some("Great service!"))
  private val review = Review(
    id = reviewId,
    userId = userId,
    targetType = targetType,
    targetId = targetId,
    rating = rating,
    text = text,
    createdAt = generatedAt,
    updatedAt = generatedAt
  )
}
