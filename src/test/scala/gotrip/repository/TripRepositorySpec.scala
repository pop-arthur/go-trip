package gotrip.repository

import cats.effect.IO
import gotrip.domain.trip.*
import gotrip.repository.trip.TripRepository
import gotrip.repository.user.UserRepository

import java.time.LocalDate

final class TripRepositorySpec extends PostgresRepositorySpecBase with RepositoryFixtures:

  repositoryTest("TripRepository creates and finds trips by owner") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val trips = TripRepository.makePostgres[IO](sessionPool)

    for
      user <- users.create(sampleUser(60))
      otherUser <- users.create(sampleUser(61, "other@example.test"))
      trip <- trips.create(sampleTrip(62, user.id).user_id, tripCreate(sampleTrip(62, user.id)))
      byUser <- trips.findByUser(user.id, trip.id)
      byOtherUser <- trips.findByUser(otherUser.id, trip.id)
      exists <- trips.existsForUser(user.id, trip.id)
    yield
      assertEquals(byUser, Some(trip))
      assertEquals(byOtherUser, None)
      assertEquals(exists, true)
  }

  repositoryTest("TripRepository lists trips by user filters") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val trips = TripRepository.makePostgres[IO](sessionPool)

    for
      user <- users.create(sampleUser(60))
      trip <- trips.create(sampleTrip(62, user.id).user_id, tripCreate(sampleTrip(62, user.id)))
      listed <- trips.listByUser(user.id, TripSearchParams(status = Some(TripStatus.Planned), fromDate = Some(LocalDate.of(2026, 7, 1)), toDate = Some(LocalDate.of(2026, 7, 31))))
    yield assertEquals(listed, List(trip))
  }

  repositoryTest("TripRepository updates trips") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val trips = TripRepository.makePostgres[IO](sessionPool)

    for
      user <- users.create(sampleUser(60))
      trip <- trips.create(sampleTrip(62, user.id).user_id, tripCreate(sampleTrip(62, user.id)))
      updated <- trips.update(user.id, trip.id, TripUpdate(title = Some(TripTitle("Summer Train Trip"))))
    yield assertEquals(updated.map(_.title), Some(TripTitle("Summer Train Trip")))
  }

  repositoryTest("TripRepository deletes trips by owner") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val trips = TripRepository.makePostgres[IO](sessionPool)

    for
      user <- users.create(sampleUser(60))
      trip <- trips.create(sampleTrip(62, user.id).user_id, tripCreate(sampleTrip(62, user.id)))
      deleted <- trips.delete(user.id, trip.id)
      byUser <- trips.findByUser(user.id, trip.id)
    yield
      assertEquals(deleted, true)
      assertEquals(byUser, None)
  }
