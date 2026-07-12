package gotrip.repository

import cats.effect.IO
import gotrip.domain.location.LocationType
import gotrip.domain.trip.*
import gotrip.repository.location.LocationRepository
import gotrip.repository.trip.TripRepository
import gotrip.repository.triplocation.TripLocationRepository
import gotrip.repository.user.UserRepository

final class TripLocationRepositorySpec extends PostgresRepositorySpecBase with RepositoryFixtures:

  repositoryTest("TripLocationRepository creates, lists, and finds route stops") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val trips = TripRepository.makePostgres[IO](sessionPool)
    val locations = LocationRepository.makePostgres[IO](sessionPool)
    val tripLocations = TripLocationRepository.makePostgres[IO](sessionPool)

    for
      user <- users.create(sampleUser(70))
      trip <- trips.create(sampleTrip(71, user.id).user_id, tripCreate(sampleTrip(71, user.id)))
      departure <- locations.create(sampleLocation(72, "Paris Gare de Lyon", LocationType.TrainStation, country = Some("France"), city = Some("Paris")))
      arrival <- locations.create(sampleLocation(73, "Milano Centrale", LocationType.TrainStation, country = Some("Italy"), city = Some("Milan")))
      firstStop <- tripLocations.create(sampleTripLocation(74, trip.id, departure.id, 1).trip_id, tripLocationCreate(sampleTripLocation(74, trip.id, departure.id, 1)), sampleTripLocation(74, trip.id, departure.id, 1).visit_order)
      secondStop <- tripLocations.create(sampleTripLocation(75, trip.id, arrival.id, 2).trip_id, tripLocationCreate(sampleTripLocation(75, trip.id, arrival.id, 2)), sampleTripLocation(75, trip.id, arrival.id, 2).visit_order)
      listed <- tripLocations.listByTrip(trip.id)
      found <- tripLocations.findInTrip(trip.id, firstStop.id)
    yield
      assertEquals(listed.map(_.id), List(firstStop.id, secondStop.id))
      assertEquals(found, Some(firstStop))
  }

  repositoryTest("TripLocationRepository exposes trip, location, visit-order, and next-order helpers") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val trips = TripRepository.makePostgres[IO](sessionPool)
    val locations = LocationRepository.makePostgres[IO](sessionPool)
    val tripLocations = TripLocationRepository.makePostgres[IO](sessionPool)

    for
      user <- users.create(sampleUser(70))
      trip <- trips.create(sampleTrip(71, user.id).user_id, tripCreate(sampleTrip(71, user.id)))
      departure <- locations.create(sampleLocation(72, "Paris Gare de Lyon", LocationType.TrainStation, country = Some("France"), city = Some("Paris")))
      firstStop <- tripLocations.create(sampleTripLocation(74, trip.id, departure.id, 1).trip_id, tripLocationCreate(sampleTripLocation(74, trip.id, departure.id, 1)), sampleTripLocation(74, trip.id, departure.id, 1).visit_order)
      tripExists <- tripLocations.tripExists(trip.id)
      tripExistsForUser <- tripLocations.tripExistsForUser(user.id, trip.id)
      locationExists <- tripLocations.locationExists(departure.id)
      visitOrderExists <- tripLocations.visitOrderExists(trip.id, VisitOrder(1))
      visitOrderExistsWithExclude <- tripLocations.visitOrderExists(trip.id, VisitOrder(1), Some(firstStop.id))
      nextVisitOrder <- tripLocations.nextVisitOrder(trip.id)
    yield
      assertEquals(tripExists, true)
      assertEquals(tripExistsForUser, true)
      assertEquals(locationExists, true)
      assertEquals(visitOrderExists, true)
      assertEquals(visitOrderExistsWithExclude, false)
      assertEquals(nextVisitOrder, VisitOrder(2))
  }

  repositoryTest("TripLocationRepository updates route stops") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val trips = TripRepository.makePostgres[IO](sessionPool)
    val locations = LocationRepository.makePostgres[IO](sessionPool)
    val tripLocations = TripLocationRepository.makePostgres[IO](sessionPool)

    for
      user <- users.create(sampleUser(70))
      trip <- trips.create(sampleTrip(71, user.id).user_id, tripCreate(sampleTrip(71, user.id)))
      arrival <- locations.create(sampleLocation(73, "Milano Centrale", LocationType.TrainStation, country = Some("Italy"), city = Some("Milan")))
      stop <- tripLocations.create(sampleTripLocation(75, trip.id, arrival.id, 2).trip_id, tripLocationCreate(sampleTripLocation(75, trip.id, arrival.id, 2)), sampleTripLocation(75, trip.id, arrival.id, 2).visit_order)
      updated <- tripLocations.update(trip.id, stop.id, TripLocationUpdate(visit_order = Some(VisitOrder(3))))
    yield assertEquals(updated.map(_.visit_order), Some(VisitOrder(3)))

  }

  repositoryTest("TripLocationRepository deletes route stops") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val trips = TripRepository.makePostgres[IO](sessionPool)
    val locations = LocationRepository.makePostgres[IO](sessionPool)
    val tripLocations = TripLocationRepository.makePostgres[IO](sessionPool)

    for
      user <- users.create(sampleUser(70))
      trip <- trips.create(sampleTrip(71, user.id).user_id, tripCreate(sampleTrip(71, user.id)))
      departure <- locations.create(sampleLocation(72, "Paris Gare de Lyon", LocationType.TrainStation, country = Some("France"), city = Some("Paris")))
      stop <- tripLocations.create(sampleTripLocation(74, trip.id, departure.id, 1).trip_id, tripLocationCreate(sampleTripLocation(74, trip.id, departure.id, 1)), sampleTripLocation(74, trip.id, departure.id, 1).visit_order)
      deleted <- tripLocations.delete(trip.id, stop.id)
      found <- tripLocations.findInTrip(trip.id, stop.id)
    yield
      assertEquals(deleted, true)
      assertEquals(found, None)
  }
