package gotrip.repository

import cats.effect.IO
import gotrip.domain.location.*
import gotrip.domain.review.*
import gotrip.repository.location.LocationRepository
import gotrip.repository.review.ReviewRepository
import gotrip.repository.user.UserRepository

final class ReviewRepositorySpec extends PostgresRepositorySpecBase with RepositoryFixtures:

  repositoryTest("ReviewRepository creates and finds reviews by id, target, and user") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val locations = LocationRepository.makePostgres[IO](sessionPool)
    val reviews = ReviewRepository.makePostgres[IO](sessionPool)

    for
      user <- users.create(sampleUser(120))
      target <- locations.create(sampleLocation(121, "Hoan Kiem Lake", LocationType.Attraction))
      reviewOne <- reviews.create(sampleReview(122, user.id, target.id, ReviewRating(5), createdAt = t(122)))
      reviewTwo <- reviews.create(sampleReview(123, user.id, target.id, ReviewRating(3), createdAt = t(123)))
      byId <- reviews.findById(reviewOne.id)
      byTarget <- reviews.findByTarget(ReviewTargetType.Location, ReviewTargetId(target.id.value))
      byUser <- reviews.findByUserId(user.id)
    yield
      assertEquals(byId, Some(reviewOne))
      assertEquals(byTarget, List(reviewTwo, reviewOne))
      assertEquals(byUser, List(reviewTwo, reviewOne))
  }

  repositoryTest("ReviewRepository calculates average rating") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val locations = LocationRepository.makePostgres[IO](sessionPool)
    val reviews = ReviewRepository.makePostgres[IO](sessionPool)

    for
      user <- users.create(sampleUser(120))
      target <- locations.create(sampleLocation(121, "Hoan Kiem Lake", LocationType.Attraction))
      _ <- reviews.create(sampleReview(122, user.id, target.id, ReviewRating(5), createdAt = t(122)))
      _ <- reviews.create(sampleReview(123, user.id, target.id, ReviewRating(3), createdAt = t(123)))
      average <- reviews.averageRating(ReviewTargetType.Location, ReviewTargetId(target.id.value))
    yield assertEquals(average, Some(4.0))
  }

  repositoryTest("ReviewRepository updates reviews") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val locations = LocationRepository.makePostgres[IO](sessionPool)
    val reviews = ReviewRepository.makePostgres[IO](sessionPool)

    for
      user <- users.create(sampleUser(120))
      target <- locations.create(sampleLocation(121, "Hoan Kiem Lake", LocationType.Attraction))
      review <- reviews.create(sampleReview(122, user.id, target.id, ReviewRating(5), createdAt = t(122)))
      updated <- reviews.update(review.copy(rating = ReviewRating(4), text = ReviewText(Some("Updated")), updatedAt = t(124)))
      found <- reviews.findById(review.id)
    yield
      assertEquals(updated, 1)
      assertEquals(found.map(_.rating), Some(ReviewRating(4)))
      assertEquals(found.flatMap(_.text.value), Some("Updated"))
  }

  repositoryTest("ReviewRepository deletes reviews") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val locations = LocationRepository.makePostgres[IO](sessionPool)
    val reviews = ReviewRepository.makePostgres[IO](sessionPool)

    for
      user <- users.create(sampleUser(120))
      target <- locations.create(sampleLocation(121, "Hoan Kiem Lake", LocationType.Attraction))
      review <- reviews.create(sampleReview(122, user.id, target.id, ReviewRating(5), createdAt = t(122)))
      deleted <- reviews.delete(review.id)
      found <- reviews.findById(review.id)
    yield
      assertEquals(deleted, 1)
      assertEquals(found, None)
  }
